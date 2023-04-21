# Spécification - extraction APASE

APASE extraction format is based on the [ICES RDB data exchange format](https://www.ices.dk/data/Documents/RDB/RDB%20Exchange%20Format.pdf).
EU/ICES have defined common format and processing tools, for fisheries statistics (.e.G R scripts - see COST project).

## Data types

In the rest of the document :
- `Req.` stands for required. In the Req. column the “M” stands for mandatory and “O” stands for optional.
- `*` = key field (warning: can be null)

## Trip (TR)

A commercial fishing trip that has been sampled on board or a sample from a fish market.

| Field name                   | Type    | Req.    | Basic checks  | Comments                                                                                                                                                                  | Écran                                      |
|------------------------------|---------|---------|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------|
| Sampling type * 	            | String  | M     	 | Code list     | “S” = sea sampling                                                                                                                                                        |                                            |
| Landing country *            | String  | M     	 | Code list     | ISO 3166 – 1 alpha-3 codes: the country where the vessel is landing and selling the catch.                                                                                |                                            |
| Vessel flag country *        | String  | M       | Code list     | ISO 3166 – 1 alpha-3 codes: the flag country of the vessel. This can be different from the landing country (see description of Landing country).                          |                                            |
| Year *                       | Integer | M       | 1 900 − 3 000 |                                                                                                                                                                           |                                            |
| Project *                    | String  | M       | Code list     | National project name. Code list is editable.                                                                                                                             |                                            |
| Trip code *                  | String  | M       | String 50     | Automatic number (Filled by system)                                                                                                                                       | Identifiant unique de la marée             |
| Vessel length                | Integer | O       | 3 − 160       | Over-all length in metres.                                                                                                                                                | cf. Référentiel > Navires                  |
| Vessel power                 | Integer | O       | 4 − 8 500     | Vessel power (kW).                                                                                                                                                        | cf. Référentiel > Navires                  | 
| Vessel size                  | Integer | O       | 1 − 2 500     | Gross registered tonnes (GRT).                                                                                                                                            | cf. Référentiel > Navires                  |
| Vessel type                  | Integer | M       | Code list     | Fixed value (4 = other boats).                                                                                                                                            |                                            |
| Harbour                      | String  | O       | Code list     | Landing harbour.                                                                                                                                                          | Calculée à partir du port de débarquement  |
| Number of sets/hauls on trip | Integer | O       | 1-300         | Total number of hauls/sets taken during the trip. Both the stations where biological measures were taken and the stations that were not worked up should be counted here. | Calculé par le système                     |
| Days at sea                  | Integer | O       | 1-60          | Difference between return and departure date, in days.                                                                                                                    | Calculé par le système                     |
| Vessel identifier            | Integer | O       | 1 − 999 999   | Vessel identifier.                                                                                                                                                        | Identifiant unique du navire (Référentiel) |
| Sampling country             | String  | M       | Code list     | ISO 3166 – 1 alpha-3 codes. The country that did the sampling.                                                                                                            |                                            |
| Sampling method              | String  | M       | Code list     | “Observer” or “SelfSampling”.                                                                                                                                             | Non saisie. Fixé à “Observer”              |

Custom columns, only for APASE:

| Field name          | Type    | Req. | Basic checks | Comments                         | Écran                  |
|---------------------|---------|------|--------------|----------------------------------|------------------------|
| Departure date time | String  | M    |              | “YYYY-MM-DD HH:MM:SS” (ISO 8601) |                        |
| Return date time    | String  | M    |              | “YYYY-MM-DD HH:MM:SS” (ISO 8601) |                        |
| Trip comments       | String  | O    |              | Comments                         |                        |
| Vessel name         | String  | M    |              | Vessel name                      | Référentiels > Navires |


## Fishing Gear (FG)

A fishing gear, with a real existence. Only one fishing gear can be deployed by an operation.

Each physical gear will have a unique identifier inside the trip: `Gear identifier`.

> Example :
> - A trip can have 2 physical gears `1 - OTB - Standard`, `2 - OTB - Selective`
> - Or have only one physical gear `1 - OTT - Twin trawl` :

For twin trawl, each sub-gear will be identified by another number: `Sub gear identifier`.

> Example :
> - An ”OTT” can have 2 sub-gears: `1 - OTT - Standard`, `2 - OTT - Selective`

| Field name                 | Type     | Req.       | Basic checks  | Comments                                                                                                                                                | Écran                                                       |
|----------------------------|----------|------------|---------------|---------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------------------------------|
| Sampling type * 	          | String   | M          | Code list     | “S” = sea sampling                                                                                                                                      |                                                             |
| Landing country *          | String   | M        	 | Code list     | ISO 3166 – 1 alpha-3 codes: the country where the vessel is landing and selling the catch.                                                              |                                                             |
| Vessel flag country *      | String   | M          | Code list     | ISO 3166 – 1 alpha-3 codes: the flag country of the vessel. This can be different from the landing country (see description of Landing country).        |                                                             |
| Year *                     | Integer  | M          | 1 900 − 3 000 |                                                                                                                                                         |                                                             |
| Project *                  | String   | M          | Code list     | National project name. Code list is editable.                                                                                                           |                                                             |
| Trip code *                | String   | M          | String 50     | National coding system.                                                                                                                                 |                                                             |
| Gear identifier *          | Integer  | M          | 1 − 99        | Each physical gear will have a unique identifier inside the trip.                                                                                       | Marée > onglet Engins > Colonne `#`                         |
| Gear type *                | String   | M          | Code list     | “OTB”  or “OTT”                                                                                                                                         |                                                             |
| Sub gear identifier        | Integer  | O          | 1 - 99        | Only for ”OTT” gear, each sub-gear are identified by a unique number.                                                                                   | Marée > onglet Engins > Engin N > Sous-engins > Colonne `#` |
| Gear label                 | String   | M          |               | Free text, but generally filled with ”STD” (standard) or ”TEST” (for selective).                                                                        | Libellé de l'engin                                          |
| Buoy weight kg             | Integer  | O          |               | Buoy weight, in kilograms                                                                                                                               | Poids des flotteurs                                         |
| Door type                  | String   | O          | Code list     | Door type                                                                                                                                               | Type de panneaux                                            |
| Entremise length           | Double   | O          |               | Entremise length, in meters                                                                                                                             | Longueur entremises                                         |
| Groundrope type            | String   | O          | Code list     | Groundrope type                                                                                                                                         |                                                             |
| Headline length            | Double   | O          |               | Headline length, in meters                                                                                                                              |                                                             |
| Mesh gauge codend mm       | Integer  | O          | 0 - 999       | Codend mesh gauge, in millimeters                                                                                                                       | Maillage - Cul (mm)                                         |
| Mesh gauge back mm         | Integer  | O          | 0 - 999       | Back mesh gauge, in millimeters                                                                                                                         | Maillage - Dos (mm)                                         |
| Mesh gauge belly mm        | Integer  | O          | 0 - 999       | Belly mesh gauge, in millimeters                                                                                                                        | Maillage - Ventre (mm)                                      |
| Mesh gauge ext mm          | Integer  | O          | 0 - 999       | Extension mesh gauge, in millimeters                                                                                                                    | Maillage - Rallonge (mm)                                    |
| Mesh gauge gor mm          | Integer  | O          | 0 - 999       | Gorget mesh gauge, in millimeters                                                                                                                       | Maillage - Gorget (mm)                                      |
| Mesh gauge lower wing mm   | Integer  | O          | 0 - 999       | Lower wing mesh gauge, in millimeters                                                                                                                   | Maillage - Ailes inférieures (mm)                           |
| Mesh gauge upper wing mm   | Integer  | O          | 0 - 999       | Upper wing mesh gauge, in millimeters                                                                                                                   | Maillage - Ailes supérieures (mm)                           |
| Nb buoy                    | Integer  | O          | 0 - 999       | Number of buoys                                                                                                                                         | Nombre de flotteurs                                         |
| Rig type                   | String   | O          | Code list     | Rig type                                                                                                                                                | Type de gréement                                            |
| Selectivity test type      | String   | O          | Code list     | For "OTB" only (Standard or selective). The type of selectivity tested during the trip. (e.g. G-NEP, G-MNZ) Used to paired Standard/Selective operation | Type d'essai de sélectivité                                 | 
| Selection device           | String   | M          | Code list     | The selection device. Mandatory for "OTB", for sub gears of "OTT". (e.g. G-NEP, G-MNZ). For standard, filled with ”NA"                                  | Dispositif sélectif                                         |
| Stone grid                 | String   | O          | Y or N        | Using a stone grid?                                                                                                                                     | Utilisation grille à cailloux ?                             |
| Sweep length               | Double   | O          |               | Sweep length, in meters                                                                                                                                 | Longueur d'un bras (m)                                      |
| Tickler chain              | String   | O          | Y or N        | Using o tickler chain?                                                                                                                                  | Utilisation racasseur/chaine ?                              |
| Vertical opening estimated | Double   | O          |               | Estimated vertical opening, in meters                                                                                                                   | Ouverture vertical estimée (m)                              |
| Vertical opening measured  | Double   | O          |               | Measured vertical opening, in meters                                                                                                                    | Ouverture vertical mesurée (m)                              |


## Fishing operation (HH)

Detailed information about a fishing operation, e.g a haul (for ”OTB” or ”OTT” gear).

| Field name                               | Type    | Req. | Basic checks                 | Comments                                                                                                              	                                                                                               | Écran                                                                            |
|------------------------------------------|---------|------|------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------|
| Sampling type * 	                        | String  | M    | Code list                    | “S” = sea sampling  	                                                                                                                                                                                                 |                                                                                  |
| Landing country *                        | String  | M    | Code list                    | ISO 3166 – 1 alpha-3 codes: the country where the vessel is landing and selling the catch.                                                                                                                            |                                                                                  |
| Vessel flag country *                    | String  | M    | Code list                    | ISO 3166 – 1 alpha-3 codes: the flag country of the vessel. This can be different from the landing country (see description of Landing country).                                                                      |                                                                                  |
| Year *                                   | Integer | M    | 1 900 − 3 000                |                                                                                                                                                                                                                       |                                                                                  |
| Project *                                | String  | M    | Code list                    | National project name. Code list is editable.                                                                                                                                                                         |                                                                                  |
| Trip code *                              | String  | M    | String 50                    | National coding system.                                                                                                                                                                                               |                                                                                  |
| Station number *                         | Integer | M    | 1-99999                      | Sequential numbering of hauls. Starting by 1 for each new trip. If the “Aggregation level” is T then this “Station number” should be 999.                                                                             |                                                                                  |
| Fishing validity                         | String  | M/O  | Code list                    | I = Invalid, V = Valid. Mandatory for sampling type “S” and “M”.                                                                                                                                                      |                                                                                  |
| Aggregation level                        | String  | M/O  | Code list                    | Fixed value ”H” (= haul).                                                                                                                                                                                             |                                                                                  |
| Catch registration                       | String  | M    | Code list                    | The parts (landings/discards) of the catch, registered as: "All", "Lan", "Dis", "None" (1).                                                                                                                           | Calculé à partir du champs "Mesure individuelles ?" (Oui => "All", Non => "Lan") |
| Species registration                     | String  | M    | Code list                    | The species in the catch, registered as "All", "Par", "None" (2). Fixed value "Par" for APASE.                                                                                                                        | Non saisi. Fixé à "Par" pour APASE                                               |
| Date                                     | String  | M    | “1900–01-01” to “2025–12–31” | “YYYY-MM-DD” (ISO 8601). Fishing starting date.                                                                                                                                                                       | Début de pêche (Fin du filage) > Date                                            |
| Time                                     | String  | O    | 00:00-23:59                  | Starting time. “HH:MM” in UTC/GTM (No daylight saving/summer time).                                                                                                                                                   | Début de pêche (Fin du filage) > Heure                                           |
| Fishing duration                         | Integer | M    | 5 − 99 999                   | In minutes.                                                                                                                                                                                                           | Calculé à partir de des dates début/fin pêche                                    |
| Pos.Start.Lat.dec.                       | Dec     | M    | 20.00000 − 80.00000          | Shooting (start) position in decimal degrees of latitude.                                                                                                                                                             | Latitude début de pêche                                                          |
| Pos.Start.Lon.dec.                       | Dec     | M    | −31.00000 − 31.00000         | Shooting (start) position in decimal degrees of longitude.                                                                                                                                                            | Longitude début de pêche                                                         |
| Pos.Stop.Lat.dec.                        | Dec     | M    | 20.00000 − 80.00000          | Hauling (stop) position in decimal degrees of latitude.                                                                                                                                                               | Latitude fin de pêche                                                            |
| Pos.Stop.Lon.dec.                        | Dec     | M    | −31.00000 − 31.00000         | Hauling (stop) position in decimal degrees of longitude.                                                                                                                                                              | Longitude fin de pêche                                                           |
| Area                                     | String  | O    | Code list                    | Not filled in APASE                                                                                                                                                                                                   |                                                                                  |
| Statistical rectangle                    | String  | M    | Code list                    | Area level 5 in the Data Collection Regulation (EC, 2008a, 2008b). This is the ICES statistical rectangles (e.g. 41G9) except for the Mediterranean and Black Seas, where GFCM geographical subareas (GSAs) are used. | Calculé à partir de la position de fin de pêche                                  |
| Subpolygon                               | String  | O    | Code list                    | Not filled in APASE                                                                                                                                                                                                   |                                                                                  |
| Main fishing depth                       | Integer | O    | 1−999                        | Depth from surface to groundrope in metres.                                                                                                                                                                           | Profondeur de l'engin (m)                                                        |
| Main water depth                         | Integer | O    | 1−999                        | Not filled in APASE                                                                                                                                                                                                   |                                                                                  |
| Fishing activity category National       | String  | O    | Code list                    | National metier                                                                                                                                                                                                       | Calculé à partir du type d'engin et de l'espèce cible                            |
| Fishing activity category European lvl 5 | String  | O    | Code list                    | Not filled in APASE                                                                                                                                                                                                   |                                                                                  |
| Fishing activity category European lvl 6 | String  | O    | Code list                    | Not filled in APASE                                                                                                                                                                                                   |                                                                                  |

Custom columns, only for APASE:

| Field name              | Type    | Req. | Basic checks | Comments                                                                           | Écran                       |
|-------------------------|---------|------|--------------|------------------------------------------------------------------------------------|-----------------------------|
| Station comments        | String  | O    |              | Free text. Mandatory if Fishing validity = "I" (Invalid)                           | Commentaires                |
| Gear identifier *       | Integer | M    |              | Gear identifier. Each physical gear will have a unique identifier inside the trip. | Engin utilisé               |
| Tag operation           | String  | O    |              | Free tag, used to paired fishing operations. Only on ”OTB” gear type               | Code de rapprochement       |
| Diurnal operation       | String  | O    | Y or N       |                                                                                    | Opération diurne ?          |
| Gear speed              | Double  | O    |              | Gear speed, in nautical knots                                                      | Vitesse de l'engin (noeuds) |
| Sea state               | String  | O    | Code list    | Douglas scale. From ”0” = calm, to ”9”  = phenomenal                               | État de la mer              |
| Wind force beaufort     | String  | O    | 0 - 12       | Beaufort scale                                                                     |                             |
| Wind cardinal direction | String  | O    | Code list    | E, N, NE, NO, O, S, SE, SO                                                         |                             |
| Rectilinear operation   | String  | O    | Y or N       | Is the operation rectilinear?                                                      | Trait rectiligne ?          |
| Seabed features         | String  | O    | Code list    | Type of substrat                                                                   | Nature du fond              |

- (1) ”Catch registration”: This field describes the fraction of the catch that was registered. If the value “None” is used, Species Registration
  must also be assigned “None” (criteria to be checked):
    - ”All” SL record is expected for both landings and discards fractions. If there is no SL record, it is a true measured 0-value.
    - "Lan" SL record is expected only for the landed fraction. For this fraction, if there is no SL record, it is a true
      measured 0-value. For the discards, no SL record is expected because it has not been registered.
    - "Dis" SL record is expected only for the discarded fraction. For this fraction, if there is no SL record, it is a true
      measured 0-value. For the landings, no SL record is ex-pected because it has not been registered.
    - "None" None. There are no SL records (criteria to be checked).

- (2) “Species registration”: This field describes whether all species or only a subset has been registered. If the value “None” is used,
  CatchRegistration must also be assigned “None”:
    - ”All” SL record is expected for all species in the given part of the catch. If there is no SL record, it is a true measured
      0-value.
    - "Par" Partial. SL record is expected only for some of the caught species. If there is no SL record, it is not known if
      it is a true measured 0-value. Please refer to the sam-pling protocol for an exact list of species which can be
      provided by the institute in charge of the given sampling.
    - "None" None. There are no SL records (criteria to be checked).

## Catch (CT)

The catch details, by sub gear

Custom columns, only for APASE:

| Field name                       | Type    | Req. | Basic checks      | Comments                                                                                                              	                          |
|----------------------------------|---------|------|-------------------|--------------------------------------------------------------------------------------------------------------------------------------------------|
| Sampling type *                  | String  | M    | Code list         | “S” = sea sampling  	                                                                                                                            |
| Landing country *                | String  | M    | Code list         | ISO 3166 – 1 alpha-3 codes: the country where the vessel is landing and selling the catch.                                                       |
| Vessel flag country *            | String  | M    | Code list         | ISO 3166 – 1 alpha-3 codes: the flag country of the vessel. This can be different from the landing country (see description of Landing country). |
| Year *                           | Integer | M    | 1 900 − 3 000     |                                                                                                                                                  | 
| Project *                        | String  | M    | Code list         | National project name. Code list is editable.                                                                                                    |
| Trip code *                      | String  | M    | String 50         | National coding system.                                                                                                                          |
| Station number *                 | Integer | M    | 1-99999           | Sequential numbering of hauls. Starting by 1 for each new trip.                                                                                  |

| Field name              | Type    | Req. | Basic checks | Comments                                                                                                    | Écran                         |
|-------------------------|---------|------|--------------|-------------------------------------------------------------------------------------------------------------|-------------------------------|
| Sub gear position *     | String  | O    | Code list    | “B” = Port side (Bâbord), “T” = Starboard side (Tribord). Mandatory for “OTT“ gears. Empty for “OTB“ gears. | Capture > Position de l'engin |
| Sub gear identifier *   | Integer | O    | 1 - 99       | Only for ”OTT” gear, each sub-gear are identified by a unique number.                                       | Capture > Sous-engin          |
| Sorting start date time | String  | O    |              | “YYYY-MM-DD HH:MM:SS” (ISO 8601)                                                                            | Capture > Début de tri        | 
| Sorting end date time   | String  | O    |              | “YYYY-MM-DD HH:MM:SS” (ISO 8601)                                                                            | Capture > Fin de tri          |   
| Catch weight *          | Integer | M    |              | The total catch weight for “OTB“ gears, or sub catch weight for “OTT“. In grams                             | Poids total de la capture     |
| Discard weight *        | Integer | M    |              | The total discard weight for “OTB“ gears, or sub discard weight for “OTT“. In grams                         | Poids total du rejet          |

## Species list (SL)

The sorting strata defined by species, catch category, etc.

| Field name                       | Type    | Req. | Basic checks      | Comments                                                                                                              	                                                                                          | Écran                                                                                                   |
|----------------------------------|---------|------|-------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|
| Sampling type *                  | String  | M    | Code list         | “S” = sea sampling  	                                                                                                                                                                                            |                                                                                                         |
| Landing country *                | String  | M    | Code list         | ISO 3166 – 1 alpha-3 codes: the country where the vessel is landing and selling the catch.                                                                                                                       |                                                                                                         |
| Vessel flag country *            | String  | M    | Code list         | ISO 3166 – 1 alpha-3 codes: the flag country of the vessel. This can be different from the landing country (see description of Landing country).                                                                 |                                                                                                         |
| Year *                           | Integer | M    | 1 900 − 3 000     |                                                                                                                                                                                                                  |                                                                                                         |
| Project *                        | String  | M    | Code list         | National project name. Code list is editable.                                                                                                                                                                    |                                                                                                         |
| Trip code *                      | String  | M    | String 50         | National coding system.                                                                                                                                                                                          |                                                                                                         |
| Station number *                 | Integer | M    | 1-99999           | Sequential numbering of hauls. Starting by 1 for each new trip. If the “Aggregation level” is T then this “Station number” should be 999.                                                                        |                                                                                                         |
| Species *                        | Integer | M    | Code list         | The AphiaID, which is a 6 digit code, is used for the species in the species field. The AphiaIDs are maintained by WoRMS. Only species AphiaIDs with status “Accepted” or “Alternate Representation” is allowed. |                                                                                                         |
| Catch category *                 | String  | M    | Code list         | The fate of the catch: “DIS” = discard, “LAN” = landing.                                                                                                                                                         |                                                                                                         |
| Landing category *               | String  | M    | Code list         | Fixed value “HUC” = human consumption.                                                                                                                                                                           |                                                                                                         |
| Commercial size category scale * | String  | O    | Code list         | Commercial sorting scale code (optional for “Unsorted”)                                                                                                                                                          | Calculé automatiquement ('Nephros' pour langoustine, "Unsorted" pour la partie rejettée, et "EU" sinon) |
| Commercial size category *       | String  | O    | Code list         | Commercial sorting category in the given scale (optional for “Unsorted”).                                                                                                                                        | Capture > Partie retenue > Colonne catégorie                                                            |
| Subsampling category *           | String  | O    | Code list         | “VRAC“ = Vrac or “H-VRAC“ = Hors Vrac. Mandatory for catch category = “DIS“                                                                                                                                      |                                                                                                         |
| Sex *                            | String  | O    | Code list         | M = Male, F = Female (optional for “Unsexed”)                                                                                                                                                                    |                                                                                                         |
| Weight                           | Integer | M    | 1 − 9 999 999 999 | Whole weight in grams. Decimals not allowed. Weight of the corresponding stratum (Species – Catch category – size category – Subsampling category - Sex).                                                        |                                                                                                         |
| Subsample weight                 | Integer | O    | 1 − 9 999 999 999 | Whole weight in grams. Decimals not allowed. For sea sampling: the live weight of the subsample of the corresponding stratum.                                                                                    |                                                                                                         |
| Length code                      |         |      |                   | Class: 1 mm = “mm”, 0.5 cm = “scm”; 1 cm = “cm”; 2.5 cm = “25 mm”, 5 cm = “5 cm”.                                                                                                                                | Référentiel > Paramètre/PSFM > Précision et unité                                                       |

Custom columns, only for APASE:

| Field name             | Type    | Req. | Basic checks | Comments                                                                                                    |
|------------------------|---------|------|--------------|-------------------------------------------------------------------------------------------------------------|
| Sub gear position *    | String  | O    | Code list    | “B” = Port side (Bâbord), “T” = Starboard side (Tribord). Mandatory for “OTT“ gears. Empty for “OTB“ gears. |
| Sub gear identifier *  | Integer | O    | 1 - 99       | Only for ”OTT” gear, each sub-gear are identified by a unique number.                                       |

## Haul length (HL)

Length frequency in the subsample of the stratum.
One record represents one length class.


| Field name                       | Type    | Req. | Basic checks  | Comments                                                                                                              	                                                                                          |
|----------------------------------|---------|------|---------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Sampling type *                  | String  | M    | Code list     | “S” = sea sampling                                                                                                                                                                                               |
| Landing country *                | String  | M    | Code list     | ISO 3166 – 1 alpha-3 codes: the country where the vessel is landing and selling the catch.                                                                                                                       |
| Vessel flag country *            | String  | M    | Code list     | ISO 3166 – 1 alpha-3 codes: the flag country of the vessel. This can be different from the landing country (see description of Landing country).                                                                 |
| Year *                           | Integer | M    | 1 900 − 3 000 |                                                                                                                                                                                                                  | 
| Project *                        | String  | M    | Code list     | National project name. Code list is editable.                                                                                                                                                                    |
| Trip code *                      | String  | M    | String 50     | National coding system.                                                                                                                                                                                          |
| Station number *                 | Integer | M    | 1-99999       | Sequential numbering of hauls. Starting by 1 for each new trip. If the “Aggregation level” is T then this “Station number” should be 999.                                                                        |
| Species  *                       | Integer | M    | Code list     | Scientific name                                                                                                                                                                                                  |
| Catch category *                 | String  | M    | Code list     | The fate of the catch: “LAN” = Landing, “BMS” = Below Minimum Size landing, “DIS” = Discard or “REGDIS” = Logbook Registered Discard.                                                                            |
| Landing category *               | String  | M    | Code list     | The intended usage at the time of landing. This should match the same field in CL record (whether or not the fish was actually used for this or another purpose): “IND” = industry or “HUC” = human consumption. |
| Commercial size category scale * | String  | O    | Code list     | Commercial sorting scale code (optional for “Unsorted”)                                                                                                                                                          |
| Commercial size category *       | String  | O    | Code list     | Commercial sorting category in the given scale (optional for “Unsorted”). (EC, 2006) and later amendments when scale is “EU”.                                                                                    |
| Subsampling category *           | String  | O    | Code list     | “VRAC“ = Vrac or “H-VRAC“ = Hors Vrac. Mandatory for catch category = “DIS“                                                                                                                                      |
| Sex                              | String  | O    | Code list     | If M = Male, = , F = Female, (optional for “Unsexed”). Only different from “Sex” if individual length distribution is obtained on HL-level (and not on SL-level).                                                |
| Length class *                   | Integer | M    | 1 − 3 999     | In mm. Identifier: lower bound of size class, e.g. 650 for 65 – 66 cm.                                                                                                                                           |
| Number at length                 | Integer | M    | 1 − 999       | (not raised to whole catch) Length classes with zero should be excluded from the record.                                                                                                                         |

Custom columns, only for APASE:

| Field name      	         | Type       | Req.   | Basic checks | Comments                                                                                                    | Écran                                     |
|---------------------------|------------|--------|--------------|-------------------------------------------------------------------------------------------------------------|-------------------------------------------|
| Sub gear position *       | String     | O      | Code list    | “B” = Port side (Bâbord), “T” = Starboard side (Tribord). Mandatory for “OTT“ gears. Empty for “OTB“ gears. |                                           |
| Sub gear identifier *     | Integer    | O      | 1 - 99       | Mandatory for “OTT“ gears. Empty for “OTB“ gears.                                                           |                                           |
| Elevated number at length | Integer    | M      | 1−999        | Elevated number at length                                                                                   | Nombre d'individu, calculé par le système |
| Measure type              | String     | M      | Code list    | Measure type. “LT“ = length total, “LC“ = length carapace, etc.                                             | Référentiels > Paramètres/PSFM > Code     |
| Measure type name         | String     | M      | Code list    | Measure type name. “Longueur totale“, etc.                                                                  | Référentiels > Paramètres/PSFM > Libellé  |
