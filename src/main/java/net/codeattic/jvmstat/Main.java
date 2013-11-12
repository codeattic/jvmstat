package net.codeattic.jvmstat;

import sun.jvmstat.monitor.HostIdentifier;
import sun.jvmstat.monitor.Monitor;
import sun.jvmstat.monitor.MonitoredHost;
import sun.jvmstat.monitor.MonitoredVm;
import sun.jvmstat.monitor.VmIdentifier;

import javax.xml.bind.DatatypeConverter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TODO - Document it.
 */
public class Main {

	private static SimpleDateFormat timestampFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS");

	/**
	 * Prints the usage syntax.
	 */
	private static void printUsage() {
		System.out.println("JVM Stats 1.0");
		System.out.println();
		System.out.println("usage: jvmstat command [options] <args>");
		System.out.println();
		System.out.println("options:");
		System.out.println("  --host          # Host ID [protocol:][[//]hostname][:port][/servername]");
		System.out.println("  --vm            # VM ID [protocol:][//]lvmid[@hostname][:port][/servername]");
		System.out.println("  --wait          # Wait time in milliseconds");
		System.out.println("  --date-format   # iso8601, millis, formatted");
		System.out.println();
		System.out.println("commands:");
		System.out.println("  vm:list                                    # Lists all the VMs");
		System.out.println("  monitor:list [<pattern1> ... <patternN>]   # Lists all the monitors");
		System.out.println();
	}

	public static String formatDate(Calendar date, String format) {
		if (format != null) {
			if (format.equals("millis")) {
				return Long.toString(date.getTimeInMillis());
			} else if (format.equals("formatted")) {
				return timestampFormat.format(date.getTime());
			}
		}
		return DatatypeConverter.printDateTime(date);
	}

	public static void vmList(List<String> params, Map<String, String> options) throws Exception {
		HostIdentifier hostId = new HostIdentifier(options.get("host"));

		MonitoredHost host = MonitoredHost.getMonitoredHost(hostId);

		System.out.println("vmid");

		Set<Integer> vmIds = host.activeVms();
		for (Integer id : vmIds) {
			System.out.println(id);
		}
	}

	public static void monitorList(List<String> params, Map<String, String> options) throws Exception {
		HostIdentifier hostId = new HostIdentifier(options.get("host"));
		VmIdentifier vmId = new VmIdentifier(options.get("vm"));
		String dateFormat = options.get("date-format");
		long wait = options.containsKey("wait") ? Long.parseLong(options.get("wait")) : -1;

		MonitoredHost host = MonitoredHost.getMonitoredHost(hostId);
		MonitoredVm vm = host.getMonitoredVm(vmId);

		if (params.size() == 1) {
			params.add(".*");
		}

		List<Monitor> monitors = new ArrayList<Monitor>();
		for (int i = 1; i < params.size(); i++) {
			monitors.addAll(vm.findByPattern(params.get(i)));
		}

		if (wait == -1) {
			System.out.println("timestamp: " + formatDate(new GregorianCalendar(), dateFormat));
			System.out.println("vmid: " + options.get("vm"));
			for (Monitor monitor : monitors) {
				System.out.println(monitor.getName() + ": " + monitor.getValue());
			}
		} else {
			System.out.print("timestamp,vmid");
			for (Monitor monitor : monitors) {
				System.out.print(",");
				System.out.print(monitor.getName());
			}
			System.out.println();

			while (true) {
				System.out.print(formatDate(new GregorianCalendar(), dateFormat));
				System.out.print(",");
				System.out.print(options.get("vm"));
				for (Monitor monitor : monitors) {
					System.out.print(",");
					System.out.print(monitor.getValue());
				}
				System.out.println();
				Thread.sleep(wait);
			}
		}
	}

	public static void main(String[] args) throws Exception {

		if (args.length == 0) {
			printUsage();
			System.exit(1);
			return;
		}

		Map<String, String> options = new LinkedHashMap<String, String>();
		List<String> params = new ArrayList<String>();

		for (int i = 0; i < args.length; i++) {
			String arg = args[i];
			if (arg.startsWith("--")) {
				String name = arg.substring(2);
				String value = args[++i];
				options.put(name, value);
			} else {
				params.add(arg);
			}
		}

		String command = params.get(0);

		if (command.equals("vm:list")) {
			vmList(params, options);
		} else if (command.equals("monitor:list")) {
			monitorList(params, options);
		}

		// HostIdentifier: [protocol:][[//]hostname][:port][/servername]
		// VmIdentifier: [protocol:][//]lvmid[@hostname][:port][/servername]

	}
}
