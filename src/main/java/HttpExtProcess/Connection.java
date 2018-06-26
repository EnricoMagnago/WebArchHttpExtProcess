package HttpExtProcess;

import ProcessRunner.RunExecutable;

import java.io.*;
import java.net.Socket;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.StringTokenizer;

/**
 * Project: HttpExtProcess
 * Created by en on 18/09/17.
 */
public class Connection extends Thread{ /* extends thread allows this class to be executed as a separate thread */

        protected final Socket sock;
        protected final Path workingDirectory;
        // subdirectories of workingDirectory where it's possible to find resources.
        private static final String textDir = "/html";
        private static final String binDir = "/process";

        public Connection(Socket sock){
                this.sock = sock;
                this.workingDirectory = Paths.get("src/main/resources").toAbsolutePath();
                // give priority to the daemon.
                this.setPriority(NORM_PRIORITY - 1);
                // start self, internally calls run.
                this.start();
        }

        /**
         * handle client interaction using sock.
         */
        @Override
        public void run(){
                try{
                        // output stream
                        final PrintStream out = new PrintStream(sock.getOutputStream());
                        // input stream
                        final BufferedReader request = new BufferedReader(new InputStreamReader(sock.getInputStream()));
                        // read the received request as a char stream (this does not work with HTTP-2.0).
                        String req = request.readLine();

                        System.out.println(sock.getInetAddress() + " -> request: " + req);
                        StringTokenizer st = new StringTokenizer(req);
                        if((st.countTokens() >= 2) && st.nextToken().equals("GET")){
                                req = st.nextToken();
                                if(req.startsWith("/process")){
                                        // handle process request.
                                        req = req.substring(8);
                                        this.handleProcessRequest(req, out);
                                }else{
                                        // handle file request.
                                        this.handleFileRequest(req, out);
                                }

                        }else{
                                new PrintStream(out).println("400 Bad Request");
                                System.out.println(sock.getInetAddress() + " -> 400 Bad Request: " + req);
                                sock.close();
                        }
                }catch(IOException e){
                        System.out.println("Generic I/O error " + e);
                }finally{
                        // close socket (closes output and input streams).
                        try{
                                sock.close();
                        }catch(IOException ex){
                                System.err.println("I/O error on socket close" + ex);
                        }
                }
        }

        /**
         * @param requestedStr : string representig the path to the requested resource
         * @return true if the given path it's inside the working directory, false otherwise.
         */
        protected boolean checkPath(String requestedStr){
                return this.checkPath(Paths.get(requestedStr).toAbsolutePath().normalize());
        }

        /**
         * @param requestedPath : absolute Path of the requested resource.
         * @return true if the given path it's inside the working directory, false otherwise.
         */
        protected boolean checkPath(Path requestedPath){
                return requestedPath.startsWith(this.workingDirectory);
        }

        private void handleFileRequest(String req, PrintStream out){
                // auto-fill request if needed.
                if(req.equals("")){ // these should never happen, here just for safety.
                        req = "/index.html";
                }else if(req.endsWith("/")){
                        req += "index.html";
                }
                // requested resource relative to the working directory.
                req = this.workingDirectory.toString() + textDir + req;
                // check that requested path is actually inside working directory (security check).
                if(!checkPath(req)){
                        out.println("Requested resource is not accessible");
                        System.out.println(sock.getInetAddress() + " -> requested a not accessible file: " + req);
                }else{

                        try{
                                FileInputStream fis = new FileInputStream(req);
                                byte[] data = new byte[fis.available()];
                                fis.read(data);
                                out.write(data);
                        }catch(FileNotFoundException e){
                                out.println("404 Not Found");
                                System.out.println("404 Not Found: " + req);

                        }catch(IOException e){
                                out.println("500 error in retrieving the requested resource, please be patient");
                                System.err.println("IOException in reading and sending requested resource : " + e);
                        }
                }
        }

        private void handleProcessRequest(String req, PrintStream out){
                ArrayList<String> args = new ArrayList<>();
                if(req.contains("?")){
                        final StringTokenizer keyValuePairs = new StringTokenizer(req.substring(req.indexOf("?") + 1), "&");
                        req = req.substring(0, req.indexOf("?"));
                        while(keyValuePairs.hasMoreTokens()){
                                final String pair = keyValuePairs.nextToken();
                                // ignore malformed pairs.
                                if(pair.contains("=")){
                                        final String key = pair.substring(0, pair.indexOf("="));
                                        try{
                                                final int index = Integer.parseInt(key.substring(3)) - 1;
                                                args.ensureCapacity(index);
                                                String value = pair.substring(pair.indexOf("=") + 1);
                                                args.add(index, value);
                                        }catch(NumberFormatException exc){
                                                // just ignore malformed parameters.
                                        }
                                }
                        }
                }
                // requested resource relative to the working directory.
                final Path executablePath = Paths.get(this.workingDirectory.toString() + binDir + req).toAbsolutePath()
                                .normalize();
                System.out.println("path: " + executablePath.toString());
                if(!checkPath(executablePath)){
                        out.println("Requested resource is not accessible");
                        System.out.println(sock.getInetAddress() + " -> requested a not accessible executable: " +
                                        executablePath);
                }else{

                        RunExecutable executable = new RunExecutable(executablePath.toString(), args);
                        try{
                                System.out.println(sock.getInetAddress() + " -> run : " + executablePath + " " + args.toString());
                                final String output = executable.execute();
                                out.println(output);
                        }catch(InterruptedException exc){
                                // mask exception
                        }catch(IOException e){
                                System.out.println("error in the process execution: " + e);
                                out.println("could not complete the execution of the process");
                        }
                }
        }
}
