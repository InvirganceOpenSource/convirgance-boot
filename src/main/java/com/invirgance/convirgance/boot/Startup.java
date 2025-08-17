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

import java.io.File;
import org.eclipse.jetty.ee10.webapp.WebAppContext;
import org.eclipse.jetty.server.Server;

/**
 *
 * @author jbanes
 */
public class Startup implements Runnable
{
    public static final int DEFAULT_PORT = 8080;
    
    private static long start = System.currentTimeMillis();
    
    private Server server;

    public Startup(Server server)
    {
        this.server = server;
    }
    
    public static void setStart(long time)
    {
        start = time;
    }
    
    private static int getPort(String[] args)
    {
        for(int i=0; i<args.length; i++)
        {
            if(args[i].equals("-p") && args.length > i+1)
            {
                return Integer.parseInt(args[i+1]);
            }
        }
        
        return DEFAULT_PORT;
    }
    
    public static void main(String[] args) throws Exception
    {
        int port = getPort(args);
        
        Server server = new Server(port);
        WebAppContext context = new WebAppContext(".virge/root", "/");
        
        System.out.println("WAR: " + context.getWar());
        
        server.setHandler(context);
        server.start();
        
        Runtime.getRuntime().addShutdownHook(new Thread(new Startup(server)));
        
        System.out.println();
        System.out.println("Application started");
        System.out.println("-------------------");
        System.out.println("Port: " + port);
        System.out.println("Startup time: " + (System.currentTimeMillis() - start) + "ms");
        
        server.join();
    }

    @Override
    public void run()
    {
        System.out.println();
        System.out.print("Shutting down... \n");
        
        try { server.stop(); } catch(Throwable t) { t.printStackTrace(); }
        
        ConvirganceBoot.cleanup();
        
        System.out.println();
        System.out.println("[SHUTDOWN COMPLETE]");
    }
}
