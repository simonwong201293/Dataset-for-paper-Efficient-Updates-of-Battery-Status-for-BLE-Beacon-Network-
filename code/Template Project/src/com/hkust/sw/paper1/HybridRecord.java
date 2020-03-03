package com.hkust.sw.paper1;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HybridRecord {
	public long time;
	public EstimatedRecord er1;
	public EstimatedRecord er2;
	public EstimatedRecord er3;
	public EstimatedRecord er4;

	public boolean checkValid(int num) {
		if (er1 == null || er2 == null || er3 == null || er4 == null)
			return true;
		List<EstimatedRecord> tmpRecord = new ArrayList<>();
		tmpRecord.add(er1);
		tmpRecord.add(er2);
		tmpRecord.add(er3);
		tmpRecord.add(er4);
		Collections.sort(tmpRecord, new Comparator<EstimatedRecord>() {
			@Override
			public int compare(EstimatedRecord o1, EstimatedRecord o2) {
				return Double.compare(o1.estimatedVoltage, o2.estimatedVoltage);
			}
		});
		return tmpRecord.get(0).beaconNo != num && tmpRecord.get(tmpRecord.size() - 1).beaconNo != num;
	}
}
