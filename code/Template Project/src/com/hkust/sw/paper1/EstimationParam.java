package com.hkust.sw.paper1;

public class EstimationParam {
	public Checkpoint cp1 = null;
	public Checkpoint cp2 = null;
	public Double reportIx = null;
	public boolean forced = false;

	public void addCheckpoint(double acceptPercent, Checkpoint cp) {
		if (cp1 == null) {
			cp1 = cp;
		} else if (cp2 == null) {
			if (cp.voltage < cp1.voltage) {
				cp2 = cp;
				reportIx = Math.abs(acceptPercent * cp1.voltage * (cp1.time - cp2.time) / (cp1.voltage - cp2.voltage));
			}
		} else if (forced || cp.time > cp2.time + reportIx) {
			cp1 = cp2;
			cp2 = cp;
			reportIx = Math.abs(acceptPercent * cp1.voltage * (cp1.time - cp2.time) / (cp1.voltage - cp2.voltage));
		}
	}

	public EstimatedRecord getNextEstimation(Checkpoint cp) {
		if (cp1 == null || cp2 == null) {
			EstimatedRecord er = new EstimatedRecord();
			er.beaconNo = cp.beaconNo;
			er.errorRate = 0.0;
			er.estimatedVoltage = cp.voltage;
			er.time = cp.time;
			return er;
		} else {
			EstimatedRecord er = new EstimatedRecord();
			er.beaconNo = cp.beaconNo;
			double slope = (cp1.voltage - cp2.voltage) / ((cp1.time - cp2.time) * 1.0);
			er.estimatedVoltage = cp1.voltage - (slope * (cp1.time - cp.time));
			er.errorRate = Math.abs(cp.voltage - er.estimatedVoltage) / cp.voltage * 100;
			er.time = cp.time;
			return er;
		}
	}

	public EstimatedRecord getNextEstimation(long time) {
		if (cp1 == null || cp2 == null) {
			return null;
		} else {
			EstimatedRecord er = new EstimatedRecord();
			er.beaconNo = cp2.beaconNo;
			double slope = (cp1.voltage - cp2.voltage) / ((cp1.time - cp2.time) * 1.0);
			er.estimatedVoltage = cp1.voltage - (slope * (cp1.time - time));
			er.errorRate = -1;
			er.time = time;
			return er;
		}
	}
}
