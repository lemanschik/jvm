/*
 * Copyright (c) 2019, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.test.builtins;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.PolyglotAccess;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static com.oracle.truffle.js.lang.JavaScriptLanguage.ID;

import static com.oracle.truffle.js.runtime.JSContextOptions.COMMONJS_REQUIRE_CWD_NAME;
import static com.oracle.truffle.js.runtime.JSContextOptions.COMMONJS_CORE_MODULES_REPLACEMENTS_NAME;
import static com.oracle.truffle.js.runtime.JSContextOptions.COMMONJS_REQUIRE_GLOBAL_PROPERTIES_NAME;
import static com.oracle.truffle.js.runtime.JSContextOptions.COMMONJS_REQUIRE_NAME;
import static com.oracle.truffle.js.runtime.JSContextOptions.GLOBAL_PROPERTY_NAME;
import static org.junit.Assert.assertEquals;

public class CommonJSRequireTest {

    private static final String PATH_OF_TESTS = "src/com.oracle.truffle.js.test/commonjs";

    private static Context testContext(Path tempFolder) {
        return testContext(tempFolder, System.out, System.err);
    }

    private static Context testContext(Map<String, String> options) {
        return testContext(System.out, System.err, options);
    }

    private static Context testContext(OutputStream out, OutputStream err, Map<String, String> options) {
        return Context.newBuilder(ID).allowPolyglotAccess(PolyglotAccess.ALL).allowExperimentalOptions(true).options(options).out(out).err(err).allowIO(true).build();
    }

    private static Context testContext(Path tempFolder, OutputStream out, OutputStream err) {
        Map<String, String> options = new HashMap<>();
        options.put(COMMONJS_REQUIRE_NAME, "true");
        options.put(COMMONJS_REQUIRE_CWD_NAME, tempFolder.toAbsolutePath().toString());
        return testContext(out, err, options);
    }

    private static Path getTestRootFolder() {
        String testPath = System.getProperty("commonjs.test.path", PATH_OF_TESTS);
        Path root = FileSystems.getDefault().getPath(testPath);
        if (!Files.exists(root)) {
            throw new AssertionError("Unable to locate test folder: " + root);
        }
        return root.toAbsolutePath();
    }

    private static Source getSourceFor(Path path) throws IOException {
        File file = new File(path.normalize().toAbsolutePath().toString());
        return Source.newBuilder("js", file).build();
    }

    private static void testBasicPackageJsonRequire(String moduleName) {
        Path f = getTestRootFolder();
        try (Context cx = testContext(f)) {
            Value js = cx.eval(ID, "require(" + moduleName + ").foo;");
            Assert.assertEquals(42, js.asInt());
        }
    }

    private static void testBasicRequire(String moduleName) {
        Path f = getTestRootFolder();
        try (Context cx = testContext(f)) {
            Value js = cx.eval(ID, "require('" + moduleName + "').foo;");
            Assert.assertEquals(42, js.asInt());
        }
    }

    private static void assertThrows(String src, String expectedMessage) {
        try {
            Path f = getTestRootFolder();
            try (Context cx = testContext(f)) {
                cx.eval(ID, src);
            }
            assert false;
        } catch (Throwable t) {
            if (!t.getClass().isAssignableFrom(PolyglotException.class)) {
                throw new AssertionError("Unexpected exception " + t);
            }
            assertEquals(expectedMessage, t.getMessage());
        }
    }

    @Test
    public void absoluteFilename() {
        Path f = getTestRootFolder();
        try (Context cx = testContext(f)) {
            Path path = Paths.get(f.toAbsolutePath().toString(), "module.js");
            Value js = cx.eval(ID, "require('" + path.toString() + "').foo;");
            Assert.assertEquals(42, js.asInt());
        }
    }

    @Test
    public void relativeFilename() {
        testBasicRequire("./module.js");
    }

    @Test
    public void relativeNoExtFilename() {
        testBasicRequire("./module");
    }

    @Test
    public void nodeModulesFolderWithPackageJson() {
        testBasicPackageJsonRequire("'with-package'");
    }

    @Test
    public void nodeModulesFolderWithPackageJson2() {
        testBasicPackageJsonRequire("'./with-package'");
    }

    @Test
    public void nodeModulesFolderWithPackageJson3() {
        testBasicPackageJsonRequire("'././with-package'");
    }

    @Test
    public void nodeModulesFolderWithPackageJsonNoMain() {
        testBasicPackageJsonRequire("'wrong-package'");
    }

    @Test
    public void nodeModulesFolderWithPackageJsonNoMain2() {
        testBasicPackageJsonRequire("'./wrong-package'");
    }

    @Test
    public void testMissingPackageJson() {
        testBasicPackageJsonRequire("'no-package'");
    }

    @Test
    public void testMissingPackageJson2() {
        testBasicPackageJsonRequire("'no-package'");
    }

    @Test
    public void nestedRequire() {
        Path f = getTestRootFolder();
        try (Context cx = testContext(f)) {
            Value js = cx.eval(ID, "require('./nested.js').foo;");
            Assert.assertEquals(42, js.asInt());
        }
    }

    @Test
    public void cyclicRequireFromMain() throws IOException {
        Path root = getTestRootFolder();
        Path testCase = Paths.get(root.normalize().toString(), "cycle_main.js");
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        try (Context cx = testContext(root, out, err)) {
            Value js = cx.eval(getSourceFor(testCase));

            out.flush();
            err.flush();
            String outPrint = new String(out.toByteArray());
            String errPrint = new String(err.toByteArray());

            String dirName = getTestRootFolder().toString() + testCase.getFileSystem().getSeparator();

            Assert.assertEquals("main starting at " + dirName + "cycle_main.js\n" +
                            "other starting at " + dirName + "cycle_other.js\n" +
                            "main.done = false\n" +
                            "other done\n" +
                            "other.done = true\n" +
                            "main done\n", outPrint);
            Assert.assertEquals("", errPrint);
            Assert.assertEquals(84, js.asInt());
        }
    }

    @Test
    public void cyclicRequire() throws IOException {
        Path f = getTestRootFolder();
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final ByteArrayOutputStream err = new ByteArrayOutputStream();
        try (Context cx = testContext(f, out, err)) {
            Value js = cx.eval(ID, "console.log('main starting');" +
                            "const a = require('./a.js');" +
                            "const b = require('./b.js');" +
                            "console.log('in main, a.done = ' + a.done + ', b.done = ' + b.done);" +
                            "42;");
            out.flush();
            err.flush();
            String outPrint = new String(out.toByteArray());
            String errPrint = new String(err.toByteArray());

            Assert.assertEquals("main starting\n" +
                            "a starting\n" +
                            "b starting\n" +
                            "in b, a.done = false\n" +
                            "b done\n" +
                            "in a, b.done = true\n" +
                            "a done\n" +
                            "in main, a.done = true, b.done = true\n", outPrint);
            Assert.assertEquals("", errPrint);
            Assert.assertEquals(42, js.asInt());
        }
    }

    @Test
    public void unknownModule() {
        assertThrows("require('unknown')", "TypeError: Cannot load CommonJS module: 'unknown'");
    }

    @Test
    public void unknownFile() {
        assertThrows("require('./unknown')", "TypeError: Cannot load CommonJS module: './unknown'");
    }

    @Test
    public void unknownFileWithExt() {
        assertThrows("require('./unknown.js')", "TypeError: Cannot load CommonJS module: './unknown.js'");
    }

    @Test
    public void unknownAbsolute() {
        assertThrows("require('/path/to/unknown.js')", "TypeError: Cannot load CommonJS module: '/path/to/unknown.js'");
    }

    @Test
    public void testLoadJson() {
        Path f = getTestRootFolder();
        try (Context cx = testContext(f)) {
            Value js = cx.eval(ID, "require('./valid.json').foo;");
            Assert.assertEquals(42, js.asInt());
        }
    }

    @Test
    public void testLoadBrokenJson() {
        Path f = getTestRootFolder();
        try (Context cx = testContext(f)) {
            cx.eval(ID, "require('./invalid.json').foo;");
            assert false;
        } catch (Throwable t) {
            if (!t.getClass().isAssignableFrom(PolyglotException.class)) {
                throw new AssertionError("Unexpected exception " + t);
            }
            assertEquals(t.getMessage(),
                            "SyntaxError: Invalid JSON: <json>:1:1 Expected " +
                                            ", or } but found n\n" +
                                            "{not_a_valid:##json}\n" +
                                            " ^");
        }
    }

    @Test
    public void testHasGlobals() {
        Path f = getTestRootFolder();
        String[] builtins = new String[]{"require", "__dirname", "__filename"};
        String[] types = new String[]{"function", "string", "string"};
        for (int i = 0; i < builtins.length; i++) {
            try (Context cx = testContext(f)) {
                Value val = cx.eval(ID, "(typeof " + builtins[i] + ");");
                Assert.assertEquals(types[i], val.asString());
            }
        }
    }

    @Test
    public void testDirnameFilenameInModule() {
        Path root = getTestRootFolder();
        Path subFolder = Paths.get(root.toAbsolutePath().toString(), "foo", "bar");
        File file = Paths.get(root.toAbsolutePath().toString(), "foo", "bar", "testFile.js").toFile();
        try (Context cx = testContext(root)) {
            Value dir = cx.eval("js", "require('./foo/bar/testDir.js').dir;");
            Assert.assertEquals(subFolder.toString(), dir.asString());
            Value fil = cx.eval("js", "require('./foo/bar/testFile.js').file;");
            Assert.assertEquals(file.getAbsolutePath(), fil.asString());
        }
    }

    @Test
    public void testGlobalDirnameFilename() throws IOException {
        Path root = getTestRootFolder();
        Path dirFile = Paths.get(root.toAbsolutePath().toString(), "foo", "bar", "dirName.js");
        Path dirName = Paths.get(root.toAbsolutePath().toString(), "foo", "bar");
        Path fileName = Paths.get(root.toAbsolutePath().toString(), "foo", "bar", "fileName.js");
        try (Context cx = testContext(root)) {
            Value dir = cx.eval(getSourceFor(dirFile));
            Assert.assertEquals(dirName.toAbsolutePath().toString(), dir.asString());
            Value fil = cx.eval(getSourceFor(fileName));
            Assert.assertEquals(fileName.toAbsolutePath().toString(), fil.asString());
        }
    }

    @Test
    public void testCwd() {
        Path root = getTestRootFolder();
        try (Context cx = testContext(root)) {
            Value val = cx.eval("js", "__dirname");
            Assert.assertEquals(root.toAbsolutePath().toString(), val.toString());
        }
    }

    @Test
    public void testWrongCwd() {
        Map<String, String> options = new HashMap<>();
        options.put(COMMONJS_REQUIRE_NAME, "true");
        options.put(COMMONJS_REQUIRE_CWD_NAME, "/wrong/or/not/existing/folder");
        try (Context cx = testContext(options)) {
            cx.eval("js", "__dirname");
            assert false : "Should throw";
        } catch (PolyglotException e) {
            Assert.assertEquals("Error: Invalid CommonJS root folder: /wrong/or/not/existing/folder", e.getMessage());
        }
    }

    @Test
    public void testResolve() throws IOException {
        Path root = getTestRootFolder();
        Path testCase = Paths.get(root.normalize().toString(), "foo", "bar", "foo.js");
        Path expected = Paths.get(root.normalize().toString(), "index.js");
        try (Context cx = testContext(root)) {
            Value js = cx.eval(getSourceFor(testCase));
            Assert.assertEquals(expected.toAbsolutePath().toString(), js.asString());
        }
    }

    @Test
    public void testCustomNodeBuiltin() {
        Path root = getTestRootFolder();
        Map<String, String> options = new HashMap<>();
        options.put(COMMONJS_REQUIRE_NAME, "true");
        options.put(COMMONJS_REQUIRE_CWD_NAME, root.toAbsolutePath().toString());
        // requiring the 'fs' and 'path' built-in modules will resolve `module.js`
        options.put(COMMONJS_CORE_MODULES_REPLACEMENTS_NAME, "path:./module,fs:./module.js");
        try (Context cx = testContext(options)) {
            Value js = cx.eval(ID, "require('path').foo + require('fs').foo;");
            Assert.assertEquals(84, js.asInt());
        }
    }

    @Test
    public void testGlobals() {
        Path root = getTestRootFolder();
        Map<String, String> options = new HashMap<>();
        options.put(GLOBAL_PROPERTY_NAME, "true");
        options.put(COMMONJS_REQUIRE_NAME, "true");
        options.put(COMMONJS_REQUIRE_CWD_NAME, root.toAbsolutePath().toString());
        // At context creation, the `test-globals` module will be required.
        options.put(COMMONJS_REQUIRE_GLOBAL_PROPERTIES_NAME, "test-globals");
        try (Context cx = testContext(options)) {
            Value js = cx.eval(ID, "process.foo;");
            Assert.assertEquals(42, js.asInt());
        }
    }

    @Test
    public void testRequireEsModuleCrashed() {
        Map<String, String> options = new HashMap<>();
        options.put(COMMONJS_REQUIRE_NAME, "true");
        try (Context cx = testContext(options)) {
            cx.eval("js", "require('./module.mjs');");
            assert false : "Should throw";
        } catch (PolyglotException e) {
            Assert.assertEquals("TypeError: Cannot load CommonJS module: './module.mjs'", e.getMessage());
        }
    }

}