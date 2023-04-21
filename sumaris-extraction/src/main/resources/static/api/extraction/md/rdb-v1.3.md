# The Regional DataBase (RDB) exchange format

Download the full documentation at : https://www.ices.dk/data/Documents/RDB/RDB%20Exchange%20Format.pdf (PDF)

## Data types and record types
The following data types are defined:
- CS = Commercial fisheries sampling
- CL = Commercial fisheries landings statistics
- CE = Commercial fisheries effort statistics.

Each of these three data types consists of data of specific record types (see below).

| Data type                                     | Record types                                                                                                               |
|-----------------------------------------------|----------------------------------------------------------------------------------------------------------------------------|
| CS (S (Commercial fisheries sampling)         | TR (trip), HH (haul header), SL (species list), HL (haul length), CA (catch aged) = SMAWL (Sex-Maturity-Age-Weight-Length) |
| CL (Commercial fisheries landings statistics) | CL (commercial fisheries landings statistics)                                                                              |
| CE (Commercial fisheries effort statistics)   | CE (Commercial fisheries effort statistics)                                                                                |


The record types are given in a specific hierarchy (Figure 1) and order within the data
file. Each data record consists of a range of data fields. The required order is given
below.

In the rest of the document :
- `Req.` stands for required. In the Req. column the “M” stands for mandatory and “O” stands for optional.
- `*` = key field (warning: can be null)

## Trip (TR)

A commercial fishing trip that has been sampled on board or a sample from a fish market.

| Field name                    | Type    | Req.    | Basic checks  | Comments                                                                                                                                                                  |
|-------------------------------|---------|---------|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Record type *                 | String  | M     	 |               | Fixed value TR.                                                                                                                                                           |
| Sampling type * 	             | String  | M     	 | Code list     | “S” = sea sampling, “M” = market sampling of known fishing trips, “D” = market sampling of mixed trips, “V” = vendor.                                                     |
| Landing country *             | String  | M     	 | Code list     | ISO 3166 – 1 alpha-3 codes: the country where the vessel is landing and selling the catch.                                                                                |
| Vessel flag country *         | String  | M       | Code list     | ISO 3166 – 1 alpha-3 codes: the flag country of the vessel. This can be different from the landing country (see description of Landing country).                          |
| Year *                        | Integer | M       | 1 900 − 3 000 |                                                                                                                                                                           | 
| Project *                     | String  | M       | Code list     | National project name. Code list is editable.                                                                                                                             |
| Trip code *                   | String  | M       | String 50     | National coding system.                                                                                                                                                   |
| Vessel length                 | Integer | O       | 3 − 160       | Over-all length in metres.                                                                                                                                                |
| Vessel power                  | Integer | O       | 4 − 8 500     | Vessel power (kW).                                                                                                                                                        |
| Vessel size                   | Integer | O       | 1 − 2 500     | Gross registered tonnes (GRT).                                                                                                                                            |
| Vessel type                   | Integer | M       | Code list     | 1 = stern trawler, 2 = side trawler, 3 = gillnetter, 4 = other boats.                                                                                                     |
| Harbour                       | String  | O       | Code list     | Landing harbour.                                                                                                                                                          |
| Number of sets/hauls on trip  | Integer | O       | 1-300         | Total number of hauls/sets taken during the trip. Both the stations where biological measures were taken and the stations that were not worked up should be counted here. |
| Days at sea                   | Integer | O       | 1-60          | In days.                                                                                                                                                                  |
| Vessel identifier (encrypted) | Integer | O       | 1 − 999 999   | Encrypted vessel identifier. Id encrypted so that no-one can map the Id to the real vessel.                                                                               |
| Sampling country              | String  | M       | Code list     | ISO 3166 – 1 alpha-3 codes. The country that did the sampling.                                                                                                            |
| Sampling method               | String  | M       | Code list     | “Observer” or “SelfSampling”.                                                                                                                                             |

> `*` = key field

## Fishing station (HH)

Detailed information about a fishing operation, e.g a haul.

> HH = Haul head


| Field name                               | Type    | Req. | Basic checks                 | Comments                                                                                                              	                                                                                                     |
|------------------------------------------|---------|------|------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Record type *   	                        | String  | M    |                              | Fixed value HH.                                                                                                       	                                                                                                     |
| Sampling type * 	                        | String  | M    | Code list                    | “S” = sea sampling, “M” = market sampling of known fishing trips, “D” = market sampling of mixed trips, “V” = vendor. 	                                                                                                     |
| Landing country *                        | String  | M    | Code list                    | ISO 3166 – 1 alpha-3 codes: the country where the vessel is landing and selling the catch.                                                                                                                                  |
| Vessel flag country *                    | String  | M    | Code list                    | ISO 3166 – 1 alpha-3 codes: the flag country of the vessel. This can be different from the landing country (see description of Landing country).                                                                            |
| Year *                                   | Integer | M    | 1 900 − 3 000                |                                                                                                                                                                                                                             | 
| Project *                                | String  | M    | Code list                    | National project name. Code list is editable.                                                                                                                                                                               |
| Trip code *                              | String  | M    | String 50                    | National coding system.                                                                                                                                                                                                     |
| Station number *                         | Integer | M    | 1-99999                      | Sequential numbering of hauls. Starting by 1 for each new trip. If the “Aggregation level” is T then this “Station number” should be 999.                                                                                   |
| Fishing validity                         | String  | M/O  | Code list                    | I = Invalid, V = Valid. Mandatory for sampling type “S” and “M”. When a haul is invalid, then no SL and HL records are allowed.                                                                                             |
| Aggregation level                        | String  | M/O  | Code list                    | H = haul, T = trip. Mandatory for sampling type “S” and “M”. If more than one station exist for the same trip, then all should be “H” (= haul).                                                                             |
| Catch registration                       | String  | M    | Code list                    | The parts (landings/discards) of the catch, registered as:"All", "Lan", "Dis", "None".                                                                                                                                      |
| Species registration                     | String  | M    | Code list                    | The species in the catch, registered as "All", "Par", "None".                                                                                                                                                               |
| Date                                     | String  | M    | “1900–01-01” to “2025–12–31” | “YYYY-MM-DD” (ISO 8601). Fishing starting date. If aggregation level is “T”, the day = day of first station no. Fishing starting date.                                                                                      |
| Time                                     | String  | O    | 00:00-23:59                  | Starting time. “HH:MM” in UTC/GTM (No daylight saving/summer time). “Meaning the time in London”. If aggregation level is “T”, the time shoot = time shot of first station no.                                              |
| Fishing duration                         |         |      | 5 − 99 999                   | In minutes.                                                                                                                                                                                                                 |
| Pos.Start.Lat.dec.                       | Dec     | O    | 20.00000 − 80.00000          | Shooting (start) position in decimal degrees of latitude.                                                                                                                                                                   |
| Pos.Start.Lon.dec.                       | Dec     | O    | −31.00000 − 31.00000         | Shooting (start) position in decimal degrees of longitude.                                                                                                                                                                  |
| Pos.Stop.Lat.dec.                        | Dec     | O    | 20.00000 − 80.00000          | Hauling (stop) position in decimal degrees of latitude.                                                                                                                                                                     |
| Pos.Stop.Lon.dec.                        | Dec     | O    | −31.00000 − 31.00000         | Hauling (stop) position in decimal degrees of longitude.                                                                                                                                                                    |
| Area                                     | String  | O    | Code list                    | Area level 3 (level 4 for Baltic, Mediterranean, and Black Seas) in the Data Collection Regulation (EC, 2008a, 2008b).                                                                                                      |
| Statistical rectangle                    | String  | O    | Code list                    | Area level 5 in the Data Collection Regulation (EC, 2008a, 2008b). This is the ICES statistical rectangles (e.g. 41G9) except for the Mediterranean and Black Seas, where GFCM geographical subareas (GSAs) are used.       |
| Subpolygon                               | String  | O    | Code list                    | National level as defined by each country as child nodes (substratification) of the ICES rectangles. It is recommended that this is coordinated internationally, e.g. through the Regional Coordination Meetings (EC RCMs). |
| Main fishing depth                       | Integer | O    | 1−999                        | Depth from surface to groundrope in metres.                                                                                                                                                                                 |
| Main water depth                         | Integer | O    | 1−999                        | Depth from surface in metres.                                                                                                                                                                                               |
| Fishing activity category National       | String  | O    | Code list                    |                                                                                                                                                                                                                             |
| Fishing activity category European lvl 5 | String  | O    | Code list                    |                                                                                                                                                                                                                             |
| Fishing activity category European lvl 6 | String  | O    | Code list                    |                                                                                                                                                                                                                             |
| Gear type                                | String  | M    | Code list                    |                                                                                                                                                                                                                             |
| Mesh size                                | Integer | O    | 1−999                        | Stretch measure.                                                                                                                                                                                                            |
| Selection device                         | Integer | O    |                              | Not mounted = 0, Exit window / selection panel = 1, grid = 2. A selection device is defined as a square-meshed panel or window that is inserted into a towed net.                                                           |
| Mesh size in selection device            | Integer | O    |                              | In mm. The mesh size of a square-meshed panel or window shall mean the largest determinable mesh size of such a panel or window.                                                                                            |

## Species list (SL)

The sorting strata defined by species, catch category, etc.

| Field name                       | Type    | Req. | Basic checks      | Comments                                                                                                              	                                                                                                                                                    |
|----------------------------------|---------|------|-------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Record type                      | String  | M    |                   | Fixed value SL.                                                                                                       	                                                                                                                                                    |
| Sampling type                    | String  | M    | Code list         | “S” = sea sampling, “M” = market sampling of known fishing trips, “D” = market sampling of mixed trips, “V” = vendor. 	                                                                                                                                                    |
| Landing country                  | String  | M    | Code list         | ISO 3166 – 1 alpha-3 codes: the country where the vessel is landing and selling the catch.                                                                                                                                                                                 |
| Vessel flag country              | String  | M    | Code list         | ISO 3166 – 1 alpha-3 codes: the flag country of the vessel. This can be different from the landing country (see description of Landing country).                                                                                                                           |
| Year                             | Integer | M    | 1 900 − 3 000     |                                                                                                                                                                                                                                                                            | 
| Project                          | String  | M    | Code list         | National project name. Code list is editable.                                                                                                                                                                                                                              |
| Trip code                        | String  | M    | String 50         | National coding system.                                                                                                                                                                                                                                                    |
| Station number                   | Integer | M    | 1-99999           | Sequential numbering of hauls. Starting by 1 for each new trip. If the “Aggregation level” is T then this “Station number” should be 999.                                                                                                                                  |
| Species                          | Integer | M    | Code list         | The AphiaID, which is a 6 digit code, is used for the species in the species field. The AphiaIDs are maintained by WoRMS. Only species AphiaIDs with status “Accepted” or “Alternate Representation” is allowed.                                                           |
| Catch category                   | String  | M    | Code list         | The fate of the catch: “LAN” = Landing, “BMS” = Below Minimum Size landing, “DIS” = Discard or “REGDIS” = Logbook Registered Discard.                                                                                                                                      |
| Landing category *               | String  | M    | Code list         | The intended usage at the time of landing. This should match the same field in CL record (whether or not the fish was actually used for this or another purpose): “IND” = industry or “HUC” = human consumption.                                                           |
| Commercial size category scale * | String  | O    | Code list         | Commercial sorting scale code (optional for “Unsorted”)                                                                                                                                                                                                                    |
| Commercial size category *       | String  | O    | Code list         | Commercial sorting category in the given scale (optional for “Unsorted”). (EC, 2006) and later amendments when scale is “EU”.                                                                                                                                              |
| Subsampling category *           | String  | O    | Code list         | Used when different fractions of the same species are subsampled at different levels. Typically used when few large specimens are taken out from the total catch before the many small fish are subsampled.                                                                |
| Sex *                            | String  | O    | Code list         | M = Male, F = Female, T = Transitional 2 (optional for “Unsexed”)                                                                                                                                                                                                          |
| Weight                           | Integer | M    | 1 − 9 999 999 999 | Whole weight in grammes. Decimals not allowed. Weight of the corresponding stratum (Species – Catch category – size category – Sex).                                                                                                                                       |
| Subsample weight                 | Integer | O    | 1 − 9 999 999 999 | Whole weight in grammes. Decimals not allowed. For sea sampling: the live weight of the subsample of the corresponding stratum. For market sampling: the sample weight is the whole weight of the fish measured (e.g. the summed weight of the fish in one or more boxes). |
| Length code                      |         |      |                   | Class: 1 mm = “mm”, 0.5 cm = “scm”; 1 cm = “cm”; 2.5 cm = “25 mm”, 5 cm = “5 cm”.                                                                                                                                                                                          |

## Haul length (HL)

Length frequency in the subsample of the stratum.
One record represents one length class.


| Field name                       | Type    | Req. | Basic checks  | Comments                                                                                                              	                                                                                          |
|----------------------------------|---------|------|---------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Record type                      | String  | M    |               | Fixed value SL.                                                                                                       	                                                                                          |
| Sampling type                    | String  | M    | Code list     | “S” = sea sampling, “M” = market sampling of known fishing trips, “D” = market sampling of mixed trips, “V” = vendor. 	                                                                                          |
| Landing country                  | String  | M    | Code list     | ISO 3166 – 1 alpha-3 codes: the country where the vessel is landing and selling the catch.                                                                                                                       |
| Vessel flag country              | String  | M    | Code list     | ISO 3166 – 1 alpha-3 codes: the flag country of the vessel. This can be different from the landing country (see description of Landing country).                                                                 |
| Year                             | Integer | M    | 1 900 − 3 000 |                                                                                                                                                                                                                  | 
| Project                          | String  | M    | Code list     | National project name. Code list is editable.                                                                                                                                                                    |
| Trip code                        | String  | M    | String 50     | National coding system.                                                                                                                                                                                          |
| Station number                   | Integer | M    | 1-99999       | Sequential numbering of hauls. Starting by 1 for each new trip. If the “Aggregation level” is T then this “Station number” should be 999.                                                                        |
| Species                          | Integer | M    | Code list     | The AphiaID, which is a 6 digit code, is used for the species in the species field. The AphiaIDs are maintained by WoRMS. Only species AphiaIDs with status “Accepted” or “Alternate Representation” is allowed. |
| Catch category                   | String  | M    | Code list     | The fate of the catch: “LAN” = Landing, “BMS” = Below Minimum Size landing, “DIS” = Discard or “REGDIS” = Logbook Registered Discard.                                                                            |
| Landing category *               | String  | M    | Code list     | The intended usage at the time of landing. This should match the same field in CL record (whether or not the fish was actually used for this or another purpose): “IND” = industry or “HUC” = human consumption. |
| Commercial size category scale * | String  | O    | Code list     | Commercial sorting scale code (optional for “Unsorted”)                                                                                                                                                          |
| Commercial size category *       | String  | O    | Code list     | Commercial sorting category in the given scale (optional for “Unsorted”). (EC, 2006) and later amendments when scale is “EU”.                                                                                    |
| Subsampling category *           | String  | O    | Code list     | Used when different fractions of the same species are subsampled at different levels. Typically used when few large specimens are taken out from the total catch before the many small fish are subsampled.      |
| Sex *                            | String  | O    | Code list     | M = Male, F = Female, T = Transitional 2 (optional for “Unsexed”)                                                                                                                                                |
| Individual sex                   | String  | O    | Code list     | If M = Male, = , F = Female, T = Transitional = (optional for “Unsexed”). Only different from “Sex” if individual length distribution is obtained on HL-level (and not on SL-level).                             |
| Length class *                   | Integer | M    | 1−3 999       | In mm. Identifier: lower bound of size class, e.g. 650 for 65 – 66 cm.                                                                                                                                           |
| Number at length *               | Integer | M    | 1−999         | (not raised to whole catch) Length classes with zero should be excluded from the record.                                                                                                                         |


## Catch aged (CA)

> CA = SMAWL (Sex-Maturity-Age-Weight-Length)

Sex-Maturity-Age-Weight distribution sampled representatively from the
length groups.

One record represents one fish.

## Commercial fisheries landings statistics (CL)

Official landings statistics with some modifiers for misreporting.

> Not supported yet

## Commercial fisheries effort statistics (CE)

Effort statistics from logbooks.

> Not supported yet