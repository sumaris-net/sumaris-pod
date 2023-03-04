# APASE exchange format

Req. stand for required. In the Req. column the “M” stands for mandatory and “O” stands for optional.

## Trip (TR)

A commercial fishing trip that has been sampled on board or a sample from a fish market.

| Field name    	   | Type   	| Req. 	 | Basic checks  | Comments                                                                                                                                         |
|---------------------|------------|--------|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| Record type   	   | String 	| M    	 |               | Fixed value TR.                                                                                                                                  |
| Sampling type 	   | String 	| M    	 | Code list     | “S” = sea sampling, “M” = market sampling of known fishing trips, “D” = market sampling of mixed trips, “V” = vendor.                            |
| Landing country	   | String     | M    	 | Code list     | ISO 3166 – 1 alpha-3 codes: the country where the vessel is landing and selling the catch.                                                       |
| Vessel flag country | String 	| M      | Code list     | ISO 3166 – 1 alpha-3 codes: the flag country of the vessel. This can be different from the landing country (see description of Landing country). |
| Year                | Integer    | M      | 1 900 − 3 000 |                                                                                                                                                  | 
| Project             | String     | M      | Code list     | National project name. Code list is editable.                                                                                                    |
| Trip code           | String     | M      | String 50     | National coding system.                                                                                                                          |
| (...)               |            |        |               |                                                                                                                                                  |

## Haul header (HH)

Detailed information about a fishing operation, e.g a haul (for OTB gear) or a sub-haul (for OTT gear).

| Field name    	      | Type    | Req. | Basic checks                 | Comments                                                                                                              	                                                        |
|----------------------|---------|------|------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Record type   	      | String  | M    |                              | Fixed value HH.                                                                                                       	                                                        |
| Sampling type 	      | String  | M    | Code list                    | “S” = sea sampling, “M” = market sampling of known fishing trips, “D” = market sampling of mixed trips, “V” = vendor. 	                                                        |
| Landing country	     | String  | M    | Code list                    | ISO 3166 – 1 alpha-3 codes: the country where the vessel is landing and selling the catch.                                                                                     |
| Vessel flag country  | String  | M    | Code list                    | ISO 3166 – 1 alpha-3 codes: the flag country of the vessel. This can be different from the landing country (see description of Landing country).                               |
| Year                 | Integer | M    | 1 900 − 3 000                |                                                                                                                                                                                | 
| Project              | String  | M    | Code list                    | National project name. Code list is editable.                                                                                                                                  |
| Trip code            | String  | M    | String 50                    | National coding system.                                                                                                                                                        |
| Station number       | Integer | M    | 1-99999                      | Sequential numbering of hauls. Starting by 1 for each new trip. If the “Aggregation level” is T then this “Station number” should be 999.                                      |
| Fishing validity     | String  | M/O  | Code list                    | I = Invalid, V = Valid. Mandatory for sampling type “S” and “M”. When a haul is invalid, then no SL and HL records are allowed.                                                |
| Aggregation level    | String  | M/O  | Code list                    | H = haul, T = trip. Mandatory for sampling type “S” and “M”. If more than one station exist for the same trip, then all should be “H” (= haul).                                |
| Catch registration   | String  | M    | Code list                    | The parts (landings/discards) of the catch, registered as:"All", "Lan", "Dis", "None".                                                                                         |
| Species registration | String  | M    | Code list                    | The species in the catch, registered as "All", "Par", "None".                                                                                                                  |
| Date                 | String  | M    | “1900–01-01” to “2025–12–31” | “YYYY-MM-DD” (ISO 8601). If aggregation level is “T”, the day = day of first station no. Fishing starting date.                                                                |
| Time                 | String  | O    | 00:00-23:59                  | Starting time. “HH:MM” in UTC/GTM (No daylight saving/summer time). “Meaning the time in London”. If aggregation level is “T”, the time shoot = time shot of first station no. |
| (...)               |          |        |               |                                                                                                                                                  |

## Species list (SL)

The sorting strata defined by species, catch category, etc.

| Field name    	         | Type    | Req. | Basic checks  | Comments                                                                                                              	                                                                                          |
|-------------------------|---------|------|---------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Record type   	         | String  | M    |               | Fixed value SL.                                                                                                       	                                                                                          |
| Sampling type 	         | String  | M    | Code list     | “S” = sea sampling, “M” = market sampling of known fishing trips, “D” = market sampling of mixed trips, “V” = vendor. 	                                                                                          |
| Landing country	        | String  | M    | Code list     | ISO 3166 – 1 alpha-3 codes: the country where the vessel is landing and selling the catch.                                                                                                                       |
| Vessel flag country     | String  | M    | Code list     | ISO 3166 – 1 alpha-3 codes: the flag country of the vessel. This can be different from the landing country (see description of Landing country).                                                                 |
| Year                    | Integer | M    | 1 900 − 3 000 |                                                                                                                                                                                                                  | 
| Project                 | String  | M    | Code list     | National project name. Code list is editable.                                                                                                                                                                    |
| Trip code               | String  | M    | String 50     | National coding system.                                                                                                                                                                                          |
| Station number          | Integer | M    | 1-99999       | Sequential numbering of hauls. Starting by 1 for each new trip. If the “Aggregation level” is T then this “Station number” should be 999.                                                                        |
| Species                 | Integer | M    | Code list     | The AphiaID, which is a 6 digit code, is used for the species in the species field. The AphiaIDs are maintained by WoRMS. Only species AphiaIDs with status “Accepted” or “Alternate Representation” is allowed. |
| Catch category          | String  | M    | Code list     | The fate of the catch: “LAN” = Landing, “BMS” = Below Minimum Size landing, “DIS” = Discard or “REGDIS” = Logbook Registered Discard.                                                                            |
| (...)                   |         |      |               |                                                                                                                                                                                                                  |
| Sub sampling category   | String  | O    | Code list     |                                                                                                                                                                                                                  |

SUBSAMPLING_CATEGORY_FREE

## Haul length (HL)

Length frequency in the subsample of the stratum.
One record represents one length class.


| Field name    	       | Type    | Req. | Basic checks  | Comments                                                                                                              	 |
|-----------------------|---------|------|---------------|-------------------------------------------------------------------------------------------------------------------------|
| (...)                 |         |      |               |                                                                                                                         |
| Sub sampling category | String  | O    | Code list     |                                                                                                                         |
| Measure type          | String  | O    | Code list     | Measure type. “LT“ = length total, “LC“ = length carapace, etc.                                                         |
