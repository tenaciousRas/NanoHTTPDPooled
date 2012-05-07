package com.longevitysoft.spherodunk.server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Vector;

import android.util.Log;

/**
 * A wrapper on top of NanoHTTPD that provides a managed connection pool. Since
 * Nano starts immediately, it immediately kills the started threads, sets up
 * the connection pool, and waits for connections on a background thread (like
 * Nano). All other functionality inherited from NanoHTTPD.
 * 
 * <p>
 * NanoHTTPD version 1.25, Copyright &copy; 2001,2005-2012 Jarno Elonen
 * (elonen@iki.fi, http://iki.fi/elonen/) and Copyright &copy; 2010 Konstantinos
 * Togias (info@ktogias.gr, http://ktogias.gr)
 * 
 * <p>
 * See {@link NanoHTTPD#LICENCE} for complete license information.
 * 
 * <p>
 * NanoHTTPDPooled version 0.5, Copyright &copy; 2012 Free Beachler
 * (fbeachler@gmail.com, http://github.com/tenaciousRas)
 * 
 * @author NanoHTTPDPooled fbeachler
 * 
 */
public class NanoHTTPDPooled extends NanoHTTPD {

	public static final String TAG = "NanoHTTPDPooled";

	/*
	 * Where worker threads stand idle
	 */
	private static Vector<Worker> threads = new Vector<Worker>();

	/**
	 * max # worker threads
	 */
	private static int workers = 12;

	private ServerSocket myServerSocket;
	private Thread myThread;

	/**
	 * @param port
	 * @param wwwroot
	 * @throws IOException
	 */
	public NanoHTTPDPooled(int port, File wwwroot) throws IOException {
		super(port, wwwroot);
		// NanoHTTPD starts a listening socket - we'll manage our own
		super.stop(); // kill Nano's threads
		for (int i = 0; i < workers; ++i) {
			Worker w = new Worker(port, wwwroot);
			(new Thread(w, "worker #" + i)).start();
			threads.addElement(w);
		}
		myServerSocket = null;
		try {
			myServerSocket = new ServerSocket(port);
		} catch (IOException e) {
			Log.e(TAG, Log.getStackTraceString(e));
		}
		myThread = new Thread(new Runnable() {
			public void run() {
				try {
					while (true)
						new HTTPSession(myServerSocket.accept());
				} catch (IOException ioe) {
				}
			}
		});
		myThread.setDaemon(true);
		myThread.start();
	}

	public static class Worker extends NanoHTTPD implements Runnable {
		final static int BUF_SIZE = 2048;

		static final byte[] EOL = { (byte) '\r', (byte) '\n' };

		/* buffer to use for requests */
		byte[] buf;
		/* Socket to client we're handling */
		private Socket s;

		public Worker(int port, File wwwroot) throws IOException {
			super(port, wwwroot);
			// NanoHTTPD starts a listening socket
			super.stop(); // kill Nano's threads
			buf = new byte[8];
			s = null;
		}

		synchronized void setSocket(Socket s) {
			this.s = s;
			notify();
		}

		public synchronized void run() {
			while (true) {
				if (s == null) {
					/* nothing to do */
					try {
						wait();
					} catch (InterruptedException e) {
						/* should not happen */
						continue;
					}
				}
				try {
					new HTTPSession(s);
				} catch (Exception e) {
					Log.e(TAG, Log.getStackTraceString(e));
				}
				/*
				 * go back in wait queue if there's fewer than numHandler
				 * connections.
				 */
				s = null;
				Vector<Worker> pool = NanoHTTPDPooled.threads;
				synchronized (pool) {
					if (pool.size() >= NanoHTTPDPooled.workers) {
						/* too many threads, exit this one */
						return;
					} else {
						pool.addElement(this);
					}
				}
			}
		}
	}

}
