package ProcessRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Project: HttpExtProcess
 * Created by en on 18/09/17.
 */
public class RunExecutable{
	private final List<String> args;

	public RunExecutable(String executablePath, List<String> args){
		this.args = new ArrayList<>();
		this.args.add(executablePath);
		this.args.addAll(args);
	}

	/**
	 * execute process identified by executablePath, passing the ordered sequence of command line arguments.
	 * no security checks are made.
	 *
	 * @return output of the process (stdout).
	 */
	public String execute() throws IOException, InterruptedException{

		ProcessBuilder pb = new ProcessBuilder(args);
		Process process = pb.start();
		// ignore stderr.
		//final BufferedReader stderr = new BufferedReader(new InputStreamReader(process.getErrorStream()));
		final BufferedReader stdout = new BufferedReader(new InputStreamReader(process.getInputStream()));
		// this is not a good implementation.
		String line = stdout.readLine();
		String output = "";
		while(line != null){
			output += line;
			line = stdout.readLine();
		}
		stdout.close();
		process.waitFor();
		return output;
	}
}
