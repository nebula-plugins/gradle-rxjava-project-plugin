package nebula.plugin.rxjavaproject

import com.google.common.io.Files
import com.google.common.io.Resources;

/**
 * Massively rewritten from: https://svn.codehaus.org/mojo/tags/animal-sniffer-parent-1.9/java-boot-classpath-detector/src/main/java/org/codehaus/mojo/animal_sniffer/jbcpd/ClasspathHelper.java
 */
//package org.codehaus.mojo.animal_sniffer.jbcpd;
/*
 * The MIT License
 *
 * Copyright (c) 2009 codehaus.org.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

public final class ClasspathHelper {

    public static List<String> main() {
        String cp = System.getProperty("sun.boot.class.path");
        if (cp == null) {
            cp = System.getProperty("java.boot.class.path");
        }

        if (cp == null) {
            throw new RuntimeException("Unable to determine classpath");
        }

        return Arrays.asList( cp.split(File.pathSeparator) );
    }

    static File copyResource(String resourcePath, File to) {
        if (to.exists()) {
            return to
        }

        def resourceUrl = Resources.getResource(resourcePath)
        def supplier = Resources.newInputStreamSupplier(resourceUrl)
        Files.copy(supplier, to)
        return to
    }


}