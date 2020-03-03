# Dataset for research paper 'Efficient Updates of Battery Status for BLE Beacon Network'
## Summary
In the whole repository, it contains the resources for the IEEE published paper 'Efficient Updates of Battery Status for BLE Beacon Network'. The paper is published at Wimob 2019, 21 - 23 Oct, 2019 in Barcelona, Spain. Referenced paper can be found at https://ieeexplore.ieee.org/document/8923435

## Abstract of the paper
Bluetooth low energy (BLE) beacon network is one of the most favored IoT infrastructures due to its flexibility and scalability. Monitoring and updating the battery statuses of the on-site BLE beacons is an essential task for reliable operation and timely maintenance of the infrastructure. However, unregulated frequent updates of the battery statuses result in stressing the beacon network management platform, possibly threatening the reliable operation of the infrastructure. Whereas too infrequent updates degrade the freshness and reliability of the updated information. Without a reliable estimation on battery status, management and timely battery replacement operation would be difficult. To address this issue, this paper presents an efficient update method of battery status for BLE beacon network that minimizes the stress on the management platform server. The proposed approach leverages the correlation in battery status information between certain beacons to reduce the number of necessary updates while retaining high accuracy. Necessary reference data estimation, reference data reliability checking, and error correction on the estimation are the three major components in the solution. An estimation model allows accurate estimation in the cold-start stage. Moreover, an error-correction model allows to check the reliability of reference data and make a correction on the estimated value.

## Resources
In the repository, resources are allocated as followed
1. Raw collected BLE advertisement data (In data directory),
2. Template code for the solution (In code directory)
