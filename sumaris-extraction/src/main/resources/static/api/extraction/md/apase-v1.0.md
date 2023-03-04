# Spéficiation du format d'extraction APASE

APASE extraction format is based on the [ICES RDB data exchange format](https://www.ices.dk/data/Documents/RDB/RDB%20Exchange%20Format.pdf).
EU/ICES have defined common format and processing tools, for fisheries statistics (.e.G R scripts - see COST project).


## Trip (TR)

A commercial fishing trip that has been sampled on board or a sample from a fish market.

| Field name                    | Type    | Req.    | Basic checks  | Comments                                                                                                                                                                  |
|-------------------------------|---------|---------|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Record type *                 | String  | M     	 |               | Fixed value ”TR”.                                                                                                                                                         |
| Sampling type * 	             | String  | M     	 | Code list     | “S” = sea sampling                                                                                                                                                        |
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

Custom columns, only for APASE:

| Field name          | Type    | Req. | Basic checks | Comments                         |
|---------------------|---------|------|--------------|----------------------------------|
| Departure date time | String  | M    |              | “YYYY-MM-DD HH:MM:SS” (ISO 8601) |
| Return date time    | String  | M    |              | “YYYY-MM-DD HH:MM:SS” (ISO 8601) |
| Trip comments       | String  | O    |              | Comments                         |


> Req. stand for required. In the Req. column the “M” stands for mandatory and “O” stands for optional.
>
> `*` = key field

## Fishing Gear (FG)

Fishing gear, used on trips

| Field name                          | Type    | Req.    | Basic checks  | Comments                                                                                                                                         |
|-------------------------------------|---------|---------|---------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| Record type *                       | String  | M     	 |               | Fixed value ”FG                                                                                                                                  |
| Sampling type * 	                   | String  | M     	 | Code list     | “S” = sea sampling, “M” = market sampling of known fishing trips, “D” = market sampling of mixed trips, “V” = vendor.                            |
| Landing country *                   | String  | M     	 | Code list     | ISO 3166 – 1 alpha-3 codes: the country where the vessel is landing and selling the catch.                                                       |
| Vessel flag country *               | String  | M       | Code list     | ISO 3166 – 1 alpha-3 codes: the flag country of the vessel. This can be different from the landing country (see description of Landing country). |
| Year *                              | Integer | M       | 1 900 − 3 000 |                                                                                                                                                  | 
| Project *                           | String  | M       | Code list     | National project name. Code list is editable.                                                                                                    |
| Trip code *                         | String  | M       | String 50     | National coding system.                                                                                                                          |
| Gear identifier *                   | Integer | M       | 1 − 999 999   | Gear identifier. Unique for the trip.                                                                                                            |
| Gear type *                         | String  | M       | Code list     | “OTB”  or “OTT”                                                                                                                                  |
| Sub gear identifier                 | Integer | O       | 1 - 99        | Sub gear identifier. Unique for the trip.                                                                                                        |
| Mesh size                           | Integer | O       | 1−999         |                                                                                                                                                  |
| Selection device                    | Integer | O       |               |                                                                                                                                                  |
| Mesh size in selection device       | Integer | O       |               |                                                                                                                                                  |
| (...)                               |         |         |               |                                                                                                                                                  |

>  TODO: add all gear's feature

## Fishing station (HH)

Detailed information about a fishing operation, e.g a haul (for OTB gear) or a sub-haul (for OTT gear).

| Field name                               | Type    | Req. | Basic checks                 | Comments                                                                                                              	                                                                                                     |
|------------------------------------------|---------|------|------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Record type *   	                        | String  | M    |                              | Fixed value ”HH”.                                                                                                        	                                                                                                  |
| Sampling type * 	                        | String  | M    | Code list                    | “S” = sea sampling  	                                                                                                                                                                                                       |
| Landing country *                        | String  | M    | Code list                    | ISO 3166 – 1 alpha-3 codes: the country where the vessel is landing and selling the catch.                                                                                                                                  |
| Vessel flag country *                    | String  | M    | Code list                    | ISO 3166 – 1 alpha-3 codes: the flag country of the vessel. This can be different from the landing country (see description of Landing country).                                                                            |
| Year *                                   | Integer | M    | 1 900 − 3 000                |                                                                                                                                                                                                                             | 
| Project *                                | String  | M    | Code list                    | National project name. Code list is editable.                                                                                                                                                                               |
| Trip code *                              | String  | M    | String 50                    | National coding system.                                                                                                                                                                                                     |
| Station number *                         | Integer | M    | 1-99999                      | Sequential numbering of hauls. Starting by 1 for each new trip. If the “Aggregation level” is T then this “Station number” should be 999.                                                                                   |
| Fishing validity                         | String  | M/O  | Code list                    | I = Invalid, V = Valid. Mandatory for sampling type “S” and “M”. When a haul is invalid, then no SL and HL records are allowed.                                                                                             |
| Aggregation level                        | String  | M/O  | Code list                    | H = haul, T = trip. Mandatory for sampling type “S” and “M”. If more than one station exist for the same trip, then all should be “H” (= haul).                                                                             |
| Catch registration                       | String  | M    | Code list                    | The parts (landings/discards) of the catch, registered as: "All", "Lan", "Dis", "None".                                                                                                                                     |
| Species registration                     | String  | M    | Code list                    | The species in the catch, registered as "All", "Par", "None".                                                                                                                                                               |
| Date                                     | String  | M    | “1900–01-01” to “2025–12–31” | “YYYY-MM-DD” (ISO 8601). Fishing starting date. If aggregation level is “T”, the day = day of first station no. Fishing starting date.                                                                                      |
| Time                                     | String  | O    | 00:00-23:59                  | Starting time. “HH:MM” in UTC/GTM (No daylight saving/summer time). “Meaning the time in London”. If aggregation level is “T”, the time shoot = time shot of first station no.                                              |
| Fishing duration                         | Integer | M    | 5 − 99 999                   | In minutes.                                                                                                                                                                                                                 |
| Pos.Start.Lat.dec.                       | Dec     | M    | 20.00000 − 80.00000          | Shooting (start) position in decimal degrees of latitude.                                                                                                                                                                   |
| Pos.Start.Lon.dec.                       | Dec     | M    | −31.00000 − 31.00000         | Shooting (start) position in decimal degrees of longitude.                                                                                                                                                                  |
| Pos.Stop.Lat.dec.                        | Dec     | M    | 20.00000 − 80.00000          | Hauling (stop) position in decimal degrees of latitude.                                                                                                                                                                     |
| Pos.Stop.Lon.dec.                        | Dec     | M    | −31.00000 − 31.00000         | Hauling (stop) position in decimal degrees of longitude.                                                                                                                                                                    |
| Area                                     | String  | O    | Code list                    | Area level 3 (level 4 for Baltic, Mediterranean, and Black Seas) in the Data Collection Regulation (EC, 2008a, 2008b).                                                                                                      |
| Statistical rectangle                    | String  | M    | Code list                    | Area level 5 in the Data Collection Regulation (EC, 2008a, 2008b). This is the ICES statistical rectangles (e.g. 41G9) except for the Mediterranean and Black Seas, where GFCM geographical subareas (GSAs) are used.       |
| Subpolygon                               | String  | O    | Code list                    | National level as defined by each country as child nodes (substratification) of the ICES rectangles. It is recommended that this is coordinated internationally, e.g. through the Regional Coordination Meetings (EC RCMs). |
| Main fishing depth                       | Integer | O    | 1−999                        | Depth from surface to groundrope in metres.                                                                                                                                                                                 |
| Main water depth                         | Integer | O    | 1−999                        | Depth from surface in metres.                                                                                                                                                                                               |
| Fishing activity category National       | String  | O    | Code list                    |                                                                                                                                                                                                                             |
| Fishing activity category European lvl 5 | String  | O    | Code list                    |                                                                                                                                                                                                                             |
| Fishing activity category European lvl 6 | String  | O    | Code list                    |                                                                                                                                                                                                                             |

Custom columns, only for APASE:

| Field name        | Type    | Req. | Basic checks | Comments         |
|-------------------|---------|------|--------------|------------------|
| Station comments  | String  | O    |              | Comments         |
| Gear identifier * | Integer | M    |              | Gear identifier. |


## Species list (SL)

The sorting strata defined by species, catch category, etc.

| Field name                       | Type    | Req. | Basic checks      | Comments                                                                                                              	                                                                                                                                                    |
|----------------------------------|---------|------|-------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Record type                      | String  | M    |                   | Fixed value ”SL”.                                                                                                        	                                                                                                                                                 |
| Sampling type                    | String  | M    | Code list         | “S” = sea sampling  	                                                                                                                                                                                                                                                      |
| Landing country                  | String  | M    | Code list         | ISO 3166 – 1 alpha-3 codes: the country where the vessel is landing and selling the catch.                                                                                                                                                                                 |
| Vessel flag country              | String  | M    | Code list         | ISO 3166 – 1 alpha-3 codes: the flag country of the vessel. This can be different from the landing country (see description of Landing country).                                                                                                                           |
| Year                             | Integer | M    | 1 900 − 3 000     |                                                                                                                                                                                                                                                                            | 
| Project                          | String  | M    | Code list         | National project name. Code list is editable.                                                                                                                                                                                                                              |
| Trip code                        | String  | M    | String 50         | National coding system.                                                                                                                                                                                                                                                    |
| Station number                   | Integer | M    | 1-99999           | Sequential numbering of hauls. Starting by 1 for each new trip. If the “Aggregation level” is T then this “Station number” should be 999.                                                                                                                                  |
| Species                          | Integer | M    | Code list         | The AphiaID, which is a 6 digit code, is used for the species in the species field. The AphiaIDs are maintained by WoRMS. Only species AphiaIDs with status “Accepted” or “Alternate Representation” is allowed.                                                           |
| Catch category                   | String  | M    | Code list         | The fate of the catch: “DIS” = discard, “LAN” = landing.                                                                                                                                                                                                                   |
| Landing category *               | String  | M    | Code list         | The intended usage at the time of landing. This should match the same field in CL record (whether or not the fish was actually used for this or another purpose): “IND” = industry or “HUC” = human consumption.                                                           |
| Commercial size category scale * | String  | O    | Code list         | Commercial sorting scale code (optional for “Unsorted”)                                                                                                                                                                                                                    |
| Commercial size category *       | String  | O    | Code list         | Commercial sorting category in the given scale (optional for “Unsorted”). (EC, 2006) and later amendments when scale is “EU”.                                                                                                                                              |
| Subsampling category *           | String  | O    | Code list         | “VRAC“ = Vrac or “H-VRAC“ = Hors Vrac. Mandatory for catch category = “DIS“                                                                                                                                                                                                |
| Sex *                            | String  | O    | Code list         | M = Male, F = Female, T = Transitional 2 (optional for “Unsexed”)                                                                                                                                                                                                          |
| Weight                           | Integer | M    | 1 − 9 999 999 999 | Whole weight in grammes. Decimals not allowed. Weight of the corresponding stratum (Species – Catch category – size category – Sex).                                                                                                                                       |
| Subsample weight                 | Integer | O    | 1 − 9 999 999 999 | Whole weight in grammes. Decimals not allowed. For sea sampling: the live weight of the subsample of the corresponding stratum. For market sampling: the sample weight is the whole weight of the fish measured (e.g. the summed weight of the fish in one or more boxes). |
| Length code                      |         |      |                   | Class: 1 mm = “mm”, 0.5 cm = “scm”; 1 cm = “cm”; 2.5 cm = “25 mm”, 5 cm = “5 cm”.                                                                                                                                                                                          |

Custom columns, only for APASE:

| Field name        | Type    | Req. | Basic checks | Comments                                                |
|-------------------|---------|------|--------------|---------------------------------------------------------|
| Sub gear position | String  | O    | Code list    | “B” = Bâbord, “T” = Tribord. Mandatory for “OTT“ gears. |
| Sub gear number   | Integer | O    | 1 - 99       | Mandatory for “OTT“ gears.                              |

## Haul length (HL)

Length frequency in the subsample of the stratum.
One record represents one length class.


| Field name                       | Type    | Req. | Basic checks  | Comments                                                                                                              	                                                                                          |
|----------------------------------|---------|------|---------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Record type                      | String  | M    |               | Fixed value ”SL”.                                                                                                        	                                                                                       |
| Sampling type                    | String  | M    | Code list     | “S” = sea sampling                                                                                                                                                                                               |
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
| Subsampling category *           | String  | O    | Code list     | “VRAC“ = Vrac or “H-VRAC“ = Hors Vrac. Mandatory for catch category = “DIS“                                                                                                                                      |
| Sex *                            | String  | O    | Code list     | M = Male, F = Female, T = Transitional 2 (optional for “Unsexed”)                                                                                                                                                |
| Individual sex                   | String  | O    | Code list     | If M = Male, = , F = Female, T = Transitional = (optional for “Unsexed”). Only different from “Sex” if individual length distribution is obtained on HL-level (and not on SL-level).                             |
| Length class *                   | Integer | M    | 1−3 999       | In mm. Identifier: lower bound of size class, e.g. 650 for 65 – 66 cm.                                                                                                                                           |
| Number at length *               | Integer | M    | 1−999         | (not raised to whole catch) Length classes with zero should be excluded from the record.                                                                                                                         |

Custom columns, only for APASE:

| Field name    	       | Type   | Req. | Basic checks | Comments                                                                                                              	 |
|-----------------------|--------|------|--------------|-------------------------------------------------------------------------------------------------------------------------|
| Measure type          | String | O    | Code list    | Measure type. “LT“ = length total, “LC“ = length carapace, etc.                                                         |
