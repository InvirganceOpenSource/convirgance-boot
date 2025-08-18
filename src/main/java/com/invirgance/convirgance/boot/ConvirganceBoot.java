/*
 * The MIT License
 *
 * Copyright 2025 INVIRGANCE LLC.
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
 */

package com.invirgance.convirgance.boot;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 *
 * @author jbanes
 */
public class ConvirganceBoot 
{
    public static final String ROOT = ".virge";
    
    private static String mainJar;
    
    public static void cleanup()
    {
        var root = new File(ROOT);
        
        if(new File(root, "lock").exists()) return;
        
        delete(root);
    }
    
    private static void delete(File directory)
    {
        if(!directory.exists()) return;
        
        for(var file : directory.listFiles())
        {
            if(file.isDirectory()) delete(file);
            else file.delete();
        }
        
        directory.delete();
    }
    
    private static void unpack(ZipInputStream in, File root) throws IOException
    {
        ZipEntry entry;
        File output;
        
        byte[] data = new byte[4906];
        int count;
        
        root.mkdirs();
        
        while((entry = in.getNextEntry()) != null)
        {
            output = new File(root, entry.getName());
            
            if(entry.isDirectory()) 
            {
                new File(root, entry.getName()).mkdirs();
                continue;
            }
            
            output.getParentFile().mkdirs();
            
            try(var out = new FileOutputStream(output))
            {
                while((count = in.read(data)) > 0)
                {
                    out.write(data, 0, count);
                }
            }
            
            in.closeEntry();
        }
    }
    
    private static void write(InputStream in, File file) throws IOException
    {
        byte[] data = new byte[4096];
        int count;
        
        file.getParentFile().mkdirs();
        
        try(var out = new FileOutputStream(file))
        {
            while((count = in.read(data)) > 0)
            {
                out.write(data, 0, count);
            }
        }
    }

    private static ClassLoader extractLibraries() throws IOException
    {
        var in = ConvirganceBoot.class.getResourceAsStream("/libraries");
        var libraries = new BufferedReader(new InputStreamReader(in));
        var urls = new ArrayList<URL>();
        var root = ConvirganceBoot.class.getClassLoader();
        
        String library;
        File file;
        
        try(var out = new PrintWriter(new FileOutputStream(ROOT + "/libraries"), false, Charset.forName("UTF-8")))
        {
            while(libraries.ready())
            {
                library = libraries.readLine();
                
                if(mainJar == null) mainJar = library;

                try(var filein = root.getResourceAsStream(library.substring(1)))
                {
                    file = new File(ROOT + library);

                    if(file == null) throw new IllegalStateException("Unable to load " + library);

                    write(filein, file);
                    out.println(library);
                    urls.add(file.toURI().toURL());
                }
            }
        }
        
        in.close();
        
        return new URLClassLoader(urls.toArray(URL[]::new));
    }
    
    
    private static ClassLoader getLibraries() throws IOException
    {
        var in = ConvirganceBoot.class.getResourceAsStream("/libraries");
        var libraries = new BufferedReader(new InputStreamReader(in));
        var urls = new ArrayList<URL>();
        
        String library;
        File file;
        
        while(libraries.ready())
        {
            library = libraries.readLine();
            file = new File(ROOT + library);
                
            if(file == null) throw new IllegalStateException("Unable to load " + library);
                
            urls.add(file.toURI().toURL());
        }
        
        in.close();
        
        return new URLClassLoader(urls.toArray(URL[]::new));
    }
    
    private static boolean getPrepareContainer(String[] args)
    {
        for(String arg : args)
        {
            if(arg.equals("--prepare-container")) return true;
            if(arg.equals("-C")) return true;
        }
        
        return false;
    }
    
    private static boolean getUnprepareContainer(String[] args)
    {
        for(String arg : args)
        {
            if(arg.equals("--unprepare-container")) return true;
            if(arg.equals("-D")) return true;
        }
        
        return false;
    }
    
    public static void main(String[] args) throws Exception
    {
        long time = System.currentTimeMillis();
        boolean prepare = getPrepareContainer(args);
        boolean unprepare = getUnprepareContainer(args);
        
        File root = new File(ROOT);
        File lock = new File(root, "lock");
        ClassLoader loader;
        Class startup;
        
        System.out.println(
"   _____                 _                                \n" +
"  / ____|               (_)                               \n" +
" | |     ___  _ ____   ___ _ __ __ _  __ _ _ __   ___ ___ \n" +
" | |    / _ \\| '_ \\ \\ / / | '__/ _` |/ _` | '_ \\ / __/ _ \\\n" +
" | |___| (_) | | | \\ V /| | | | (_| | (_| | | | | (_|  __/\n" +
"  \\_____\\___/|_| |_|\\_/ |_|_|  \\__, |\\__,_|_| |_|\\___\\___|\n" +
"                                __/ |                     \n" +
"                               |___/                      \n");

        
        if(unprepare) 
        {
            lock.delete();
            
            
            System.out.println();
            System.out.println("Container operations cleaned up. Ready for normal operation.");
            System.out.println();
            
            return;
        }
        
        if(lock.exists())
        {
            loader = getLibraries();
        }
        else
        {
            delete(root);

            try(var in = new ZipInputStream(ConvirganceBoot.class.getResourceAsStream("/root.war")))
            {
                unpack(in, new File(root, "root"));
            }
            
            loader = extractLibraries();
        }
        
        if(prepare) 
        {
            new FileOutputStream(lock).close();
            
            System.out.println();
            System.out.println("Prepared for container operation.");
            System.out.println();
            
            return;
        }
        
        Thread.currentThread().setContextClassLoader(loader);

        startup = loader.loadClass("com.invirgance.convirgance.boot.Startup");

        startup.getMethod("setStart", long.class).invoke(null, time);
        startup.getMethod("main", String[].class).invoke(null, (Object)args);
    }
}
