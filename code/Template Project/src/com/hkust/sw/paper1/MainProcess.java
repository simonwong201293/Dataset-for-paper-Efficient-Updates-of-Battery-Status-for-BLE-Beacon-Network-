package com.hkust.sw.paper1;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class MainProcess {

	private static String inputFilePath = null;
	private static String outputFilePath = null;
	private static HashMap<Integer, List<Checkpoint>> beaconCheckpoints = new HashMap<>();

	public static void main(String[] args) {
		// Setup I/O configuration
		System.out.println("Please input file path for computational simulation");
		// Scanner input = new Scanner(System.in);
		// inputFilePath = input.nextLine();
		inputFilePath = "Your source file";
		System.out.println("Selected input File Path = " + inputFilePath);
		System.out.println("Please output folder path for computational simulation");
		// outputFilePath = input.nextLine();
		outputFilePath = "Your target destination";
		System.out.println("Selected output Folder Path = " + outputFilePath);
		// input.close();
		if (inputFilePath == null || outputFilePath == null) {
			System.out.println("Invalid I/O Path");
			return;
		}
		// Parse Data
		try {
			parseData();
		} catch (Exception ignored) {
			System.out.println("parseData Error: " + ignored.toString());
			return;
		}
		// Generate a graph for all raw data
		try {
			generateGraphForAllData();
		} catch (Exception ignored) {
			System.out.println("generateGraphForAllData Error: " + ignored.toString());
			return;
		}
		// Generate all comparison graph for only 1 checkpoint and fixed report interval
		// checkpoint for all 4 beacons
		HashMap<Integer, HashMap<Long, EstimatedRecord>> estimatedData = new HashMap<>();
		try {
			estimatedData = generateCheckpointVaryComparison(0.01);
		} catch (Exception ignored) {
			System.out.println("generateCheckpointVaryComparison Error: " + ignored.toString());
			return;
		}
		// Generate all Error Checking Model
		try {
			problemCheckpointEstimation(0.01);
		}catch(Exception ignored) {
			System.out.println("problemCheckpointEstimation Error: " + ignored.toString());
			return;
		}
	}

	private static void parseData() throws Exception {
		File file = new File(inputFilePath);
		if (!file.exists()) {
			throw new Exception("Invalid Input File");
		}
		try {
			Scanner sc = new Scanner(file);
			int line = 0;
			while (sc.hasNextLine()) {
				String lineStr = sc.nextLine();
				if (line > 0) {
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
					if (lineStr != null && lineStr.length() > 0) {
						String split[] = lineStr.split(",");
						if (split.length < 2)
							continue;
						Checkpoint cp = new Checkpoint();
						cp.voltage = Double.parseDouble(split[0]);
						cp.beaconNo = Integer.parseInt(split[1]);
						cp.time = sdf.parse(split[2]).getTime();
						if (beaconCheckpoints.get(cp.beaconNo) == null) {
							beaconCheckpoints.put(cp.beaconNo, new ArrayList<>());
						}
						beaconCheckpoints.get(cp.beaconNo).add(cp);
					}
				}
				line++;
			}
			sc.close();
		} catch (Exception e) {
			throw e;
		}
		if (beaconCheckpoints.size() <= 0) {
			throw new Exception("No Data");
		}
		for (Integer key : beaconCheckpoints.keySet()) {
			System.out.println("**CheckPoint** Beacon #" + key + " record size: " + beaconCheckpoints.get(key).size());
		}
	}

	private static void generateGraphForAllData() throws Exception {
		List<Checkpoint> sortRawData = new ArrayList<>();
		for (Integer key : beaconCheckpoints.keySet()) {
			sortRawData.addAll(beaconCheckpoints.get(key));
		}
		Collections.sort(sortRawData, new Comparator<Checkpoint>() {
			@Override
			public int compare(Checkpoint a, Checkpoint b) {
				return Long.compare(a.time, b.time);
			}
		});
		File dir = new File(outputFilePath);
		dir.mkdirs();
		File rawDataFile = new File(dir, "raw_data_graph.csv");
		if (rawDataFile.exists())
			rawDataFile.delete();
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(rawDataFile));
			long startTime = sortRawData.get(0).time;
			writer.write("time,beacon1 cap, beacon2 cap, beacon3 cap, beacon4 cap\n");
			for (Checkpoint cp : sortRawData) {
				writer.write(String.valueOf(cp.time - startTime) + ",");
				switch (cp.beaconNo) {
				case 1:
					writer.write(String.valueOf(cp.voltage) + ",,,");
					break;
				case 2:
					writer.write("," + String.valueOf(cp.voltage) + ",,");
					break;
				case 3:
					writer.write(",," + String.valueOf(cp.voltage) + ",");
					break;
				case 4:
					writer.write(",,," + String.valueOf(cp.voltage));
					break;
				}
				writer.write("\n");
			}
			writer.close();
			System.out.println("generateGraphForAllData Success: " + rawDataFile.getAbsolutePath());
		} catch (Exception e) {
			throw e;
		}
	}

	private static HashMap<Integer, HashMap<Long, EstimatedRecord>> generateCheckpointVaryComparison(
			double acceptPercent) throws Exception {
		HashMap<Integer, HashMap<Long, EstimatedRecord>> result = new HashMap<>();
		for (Integer key : beaconCheckpoints.keySet()) {
			List<Checkpoint> records = beaconCheckpoints.get(key);
			Collections.sort(records, new Comparator<Checkpoint>() {
				@Override
				public int compare(Checkpoint o1, Checkpoint o2) {
					return Long.compare(o1.time, o2.time);
				}
			});
			// find checkpoint for first 2 estimation
			double oldVal = records.get(0).voltage;
			int firstVal = -1, secondVal = -1;
			for (int i = 0; i < records.size(); i++) {
				if (firstVal == -1 && records.get(i).voltage < oldVal) {
					firstVal = i;
					oldVal = records.get(i).voltage;
				} else if (secondVal == -1 && records.get(i).voltage < oldVal) {
					secondVal = i;
				}
			}
			// take first 2 checkpoints
			HashMap<Long, EstimatedRecord> estimatedRecord1 = new HashMap<>();
			Checkpoint firstCp = records.get(0);
			Checkpoint secondCp = records.get(firstVal);
			double slope = (firstCp.voltage - secondCp.voltage) / ((firstCp.time - secondCp.time) * 1.0);
			for (Checkpoint cp : records) {
				if (cp.time >= secondCp.time) {
					EstimatedRecord er = new EstimatedRecord();
					er.beaconNo = cp.beaconNo;
					er.time = cp.time;
					er.estimatedVoltage = firstCp.voltage - (slope * (firstCp.time - cp.time));
					er.errorRate = Math.abs(cp.voltage - er.estimatedVoltage) / cp.voltage * 100;
					estimatedRecord1.put(cp.time, er);
				}
			}
			// take second 2 checkpoints
			HashMap<Long, EstimatedRecord> estimatedRecord2 = new HashMap<>();
			firstCp = records.get(1);
			secondCp = records.get(secondVal);
			slope = (firstCp.voltage - secondCp.voltage) / ((firstCp.time - secondCp.time) * 1.0);
			for (Checkpoint cp : records) {
				if (cp.time >= secondCp.time) {
					EstimatedRecord er = new EstimatedRecord();
					er.beaconNo = cp.beaconNo;
					er.time = cp.time;
					er.estimatedVoltage = firstCp.voltage - (slope * (firstCp.time - cp.time));
					er.errorRate = Math.abs(cp.voltage - er.estimatedVoltage) / cp.voltage * 100;
					estimatedRecord2.put(cp.time, er);
				}
			}
			// take self recoverable checkpoints (proposed P_A)
			HashMap<Long, EstimatedRecord> estimatedRecord3 = new HashMap<>();
			firstCp = records.get(0);
			secondCp = records.get(firstVal);
			double reportInterval = Math.abs(acceptPercent * firstCp.voltage * (firstCp.time - secondCp.time)
					/ (firstCp.voltage - secondCp.voltage));
			// System.out.println("Report interval estimated = " + reportInterval);
			slope = (firstCp.voltage - secondCp.voltage) / ((firstCp.time - secondCp.time) * 1.0);
			for (int i = 0; i < records.size(); i++) {
				if (records.get(i).time >= secondCp.time) {
					if (records.get(i).time > secondCp.time + reportInterval) {
						firstCp = secondCp;
						secondCp = records.get(i);
						reportInterval = Math.abs(acceptPercent * firstCp.voltage * (firstCp.time - secondCp.time)
								/ (firstCp.voltage - secondCp.voltage));

						// System.out.println("Report interval estimated = " + reportInterval);
						slope = (firstCp.voltage - secondCp.voltage) / ((firstCp.time - secondCp.time) * 1.0);
					}
					EstimatedRecord er = new EstimatedRecord();
					er.beaconNo = records.get(i).beaconNo;
					er.time = records.get(i).time;
					er.estimatedVoltage = firstCp.voltage - (slope * (firstCp.time - records.get(i).time));
					er.errorRate = Math.abs(records.get(i).voltage - er.estimatedVoltage) / records.get(i).voltage
							* 100;
					estimatedRecord3.put(records.get(i).time, er);
				}
			}
			result.put(key, estimatedRecord3);
			// take self recoverable checkpoints (proposed P_A * 2)
			HashMap<Long, EstimatedRecord> estimatedRecord4 = new HashMap<>();
			firstCp = records.get(0);
			secondCp = records.get(firstVal);
			reportInterval = Math.abs(acceptPercent * 2 * firstCp.voltage * (firstCp.time - secondCp.time)
					/ (firstCp.voltage - secondCp.voltage));
			// System.out.println("Report interval estimated = " + reportInterval);
			slope = (firstCp.voltage - secondCp.voltage) / ((firstCp.time - secondCp.time) * 1.0);
			for (int i = 0; i < records.size(); i++) {
				if (records.get(i).time >= secondCp.time) {
					if (records.get(i).time > secondCp.time + reportInterval) {
						firstCp = secondCp;
						secondCp = records.get(i);
						reportInterval = Math.abs(acceptPercent * 2 * firstCp.voltage * (firstCp.time - secondCp.time)
								/ (firstCp.voltage - secondCp.voltage));

						// System.out.println("Report interval estimated = " + reportInterval);
						slope = (firstCp.voltage - secondCp.voltage) / ((firstCp.time - secondCp.time) * 1.0);
					}
					EstimatedRecord er = new EstimatedRecord();
					er.beaconNo = records.get(i).beaconNo;
					er.time = records.get(i).time;
					er.estimatedVoltage = firstCp.voltage - (slope * (firstCp.time - records.get(i).time));
					er.errorRate = Math.abs(records.get(i).voltage - er.estimatedVoltage) / records.get(i).voltage
							* 100;
					estimatedRecord4.put(records.get(i).time, er);
				}
			}
			// take self recoverable checkpoints (proposed P_A * 3)
			HashMap<Long, EstimatedRecord> estimatedRecord5 = new HashMap<>();
			firstCp = records.get(0);
			secondCp = records.get(firstVal);
			reportInterval = Math.abs(acceptPercent * 3 * firstCp.voltage * (firstCp.time - secondCp.time)
					/ (firstCp.voltage - secondCp.voltage));
			// System.out.println("Report interval estimated = " + reportInterval);
			slope = (firstCp.voltage - secondCp.voltage) / ((firstCp.time - secondCp.time) * 1.0);
			for (int i = 0; i < records.size(); i++) {
				if (records.get(i).time >= secondCp.time) {
					if (records.get(i).time > secondCp.time + reportInterval) {
						firstCp = secondCp;
						secondCp = records.get(i);
						reportInterval = Math.abs(acceptPercent * 3 * firstCp.voltage * (firstCp.time - secondCp.time)
								/ (firstCp.voltage - secondCp.voltage));

						// System.out.println("Report interval estimated = " + reportInterval);
						slope = (firstCp.voltage - secondCp.voltage) / ((firstCp.time - secondCp.time) * 1.0);
					}
					EstimatedRecord er = new EstimatedRecord();
					er.beaconNo = records.get(i).beaconNo;
					er.time = records.get(i).time;
					er.estimatedVoltage = firstCp.voltage - (slope * (firstCp.time - records.get(i).time));
					er.errorRate = Math.abs(records.get(i).voltage - er.estimatedVoltage) / records.get(i).voltage
							* 100;
					estimatedRecord5.put(records.get(i).time, er);
				}
			}
			File dir = new File(outputFilePath);
			dir.mkdirs();
			File rawDataFile = new File(dir, "self_estimation_" + key + ".csv");
			if (rawDataFile.exists())
				rawDataFile.delete();
			try {
				BufferedWriter writer = new BufferedWriter(new FileWriter(rawDataFile));
				long startTime = records.get(0).time;
				writer.write(
						"time, raw, est1 cap, est1 err, est2 cap, est2 err, self est 1 cap, self est 1 err, self est2 cap, self est 2 err, self est3 cap, self est 3 err\n");
				for (Checkpoint cp : records) {
					if (!estimatedRecord1.containsKey(cp.time) && !estimatedRecord2.containsKey(cp.time)
							&& !estimatedRecord3.containsKey(cp.time))
						continue;
					writer.write(String.valueOf(cp.time - startTime) + "," + String.valueOf(cp.voltage) + ",");
					writer.write(
							estimatedRecord1.containsKey(cp.time)
									? estimatedRecord1.get(cp.time).estimatedVoltage + ","
											+ estimatedRecord1.get(cp.time).errorRate + ","
									: ",,");
					writer.write(
							estimatedRecord2.containsKey(cp.time)
									? estimatedRecord2.get(cp.time).estimatedVoltage + ","
											+ estimatedRecord2.get(cp.time).errorRate + ","
									: ",,");
					writer.write(
							estimatedRecord3.containsKey(cp.time)
									? estimatedRecord3.get(cp.time).estimatedVoltage + ","
											+ estimatedRecord3.get(cp.time).errorRate + ","
									: ",,");
					writer.write(
							estimatedRecord4.containsKey(cp.time)
									? estimatedRecord4.get(cp.time).estimatedVoltage + ","
											+ estimatedRecord4.get(cp.time).errorRate + ","
									: ",,");
					writer.write(
							estimatedRecord5.containsKey(cp.time)
									? estimatedRecord5.get(cp.time).estimatedVoltage + ","
											+ estimatedRecord5.get(cp.time).errorRate + ","
									: ",,");
					writer.write("\n");
				}
				writer.close();
				System.out.println("generateCheckpointVaryComparison Success: " + rawDataFile.getAbsolutePath());
			} catch (Exception e) {
				throw e;
			}
		}
		return result;
	}

	private static void problemCheckpointEstimation(double acceptPercent) throws Exception {
		List<Checkpoint> sortRawData = new ArrayList<>();
		for (Integer key : beaconCheckpoints.keySet()) {
			sortRawData.addAll(beaconCheckpoints.get(key));
		}
		Collections.sort(sortRawData, new Comparator<Checkpoint>() {
			@Override
			public int compare(Checkpoint a, Checkpoint b) {
				return Long.compare(a.time, b.time);
			}
		});
		EstimationParam ep1 = new EstimationParam(), ep2 = new EstimationParam(), ep3 = new EstimationParam(),
				ep4 = new EstimationParam();
		List<HybridRecord> result = new ArrayList<>();
		for (Checkpoint cp : sortRawData) {
			HybridRecord hr = new HybridRecord();
			switch (cp.beaconNo) {
			case 1:
				ep1.addCheckpoint(acceptPercent, cp);
				hr.time = cp.time;
				hr.er1 = ep1.getNextEstimation(cp);
				hr.er2 = ep2.getNextEstimation(cp.time);
				hr.er3 = ep3.getNextEstimation(cp.time);
				hr.er4 = ep4.getNextEstimation(cp.time);
				ep1.forced = !hr.checkValid(1);
				break;
			case 2:
				ep2.addCheckpoint(acceptPercent, cp);
				hr.time = cp.time;
				hr.er2 = ep2.getNextEstimation(cp);
				hr.er1 = ep1.getNextEstimation(cp.time);
				hr.er3 = ep3.getNextEstimation(cp.time);
				hr.er4 = ep4.getNextEstimation(cp.time);
				ep2.forced = !hr.checkValid(2);
				break;
			case 3:
				ep3.addCheckpoint(acceptPercent, cp);
				hr.time = cp.time;
				hr.er3 = ep3.getNextEstimation(cp);
				hr.er1 = ep1.getNextEstimation(cp.time);
				hr.er2 = ep2.getNextEstimation(cp.time);
				hr.er4 = ep4.getNextEstimation(cp.time);
				ep3.forced = !hr.checkValid(3);
				break;
			case 4:
				ep4.addCheckpoint(acceptPercent, cp);
				hr.time = cp.time;
				hr.er4 = ep4.getNextEstimation(cp);
				hr.er1 = ep1.getNextEstimation(cp.time);
				hr.er2 = ep2.getNextEstimation(cp.time);
				hr.er3 = ep3.getNextEstimation(cp.time);
				ep4.forced = !hr.checkValid(4);
				break;
			}
			result.add(hr);
		}
		File dir = new File(outputFilePath);
		dir.mkdirs();
		File rawDataFile = new File(dir, "hybrid_sol.csv");
		if (rawDataFile.exists())
			rawDataFile.delete();
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(rawDataFile));
			long startTime = sortRawData.get(0).time;
			writer.write(
					"time, b1 est, b1 error, b2 est, b2 error, b3 est, b3 error, b4 est, b4 error\n");
			for (HybridRecord hr : result) {
				if (hr.er1 == null && hr.er2 == null && hr.er3 == null && hr.er4 == null)
					continue;
				writer.write(String.valueOf(hr.time - startTime) + ",");
				writer.write(
						hr.er1 != null
								? hr.er1.estimatedVoltage + ","
										+ (hr.er1.errorRate == -1 ? "" : hr.er1.errorRate) + ","
								: ",,");
				writer.write(
						hr.er2 != null
								? hr.er2.estimatedVoltage + ","
										+ (hr.er2.errorRate == -1 ? "" : hr.er2.errorRate) + ","
								: ",,");
				writer.write(
						hr.er3 != null
								? hr.er3.estimatedVoltage + ","
										+  (hr.er3.errorRate == -1 ? "" : hr.er3.errorRate) + ","
								: ",,");
				writer.write(
						hr.er4 != null
								? hr.er4.estimatedVoltage + ","
										+  (hr.er4.errorRate == -1 ? "" : hr.er4.errorRate) + ","
								: ",,");
				writer.write("\n");
			}
			writer.close();
			System.out.println("problemCheckpointEstimation Success: " + rawDataFile.getAbsolutePath());
		} catch (Exception e) {
			throw e;
		}
	}
}
