## SQL insert for FRA Harbours

Run this queries from a Oracle SIH-Adagio (aka Harmonie) database instance:

- Insert `LOCATION`
```sql
select
  'insert into LOCATION (ID, LABEL, NAME, COMMENTS, STATUS_FK, VALIDITY_STATUS_FK, LOCATION_LEVEL_FK, CREATION_DATE, UPDATE_DATE)'
  || ' values (nextval(''location_seq''), '
  || ''''
  || DECODE(LABEL,
      'FR_FRPHQ', 'FRPHQ', -- Pornichet
      'FR_XMX', 'FRMXN', -- Morlaix
      'FR_LNA', 'FRICN', -- Saint-Brévin-l'Océan
      'FR_FNO', 'FRCE9', -- Le Collet
      'FR_SCN', 'FRCAW', -- Cabourg
      LABEL) || ''', '
  || '''' || REPLACE(NAME, '''', '''''') || ''', '
  || '''' || COMMENTS || ''', '
  || STATUS_FK || ', '
  || VALIDITY_STATUS_FK || ', '
  || LOCATION_LEVEL_FK || ', '
  || 'current_timestamp, current_timestamp);'
from (
      select distinct OI.RANK,
                      NVL(TI_LOCODE.EXTERNAL_CODE, 'FR_' || L.LABEL) as LABEL,
                      L.NAME as NAME,
                      L.LABEL as COMMENTS, -- Harmonie Label
                      L.STATUS_FK as STATUS_FK,
                      L.VALIDITY_STATUS_FK as VALIDITY_STATUS_FK,
                      2 as LOCATION_LEVEL_FK /*Harbour*/
      from LOCATION L
               inner join LOCATION_HIERARCHY LH on LH.CHILD_LOCATION_FK = L.ID
               inner join LOCATION L_COUNTRY on L_COUNTRY.ID = LH.PARENT_LOCATION_FK
          and L_COUNTRY.LOCATION_LEVEL_FK = 21 and L_COUNTRY.LABEL = 'FRA'
               left outer join TRANSCRIBING_ITEM TI_LOCODE on L.ID = TI_LOCODE.OBJECT_ID
          and TI_LOCODE.TRANSCRIBING_ITEM_TYPE_FK = 125/*ERS_LOCODE.CODE*/
               left outer join ORDER_ITEM OI on OI.OBJECT_ID = L.ID and OI.ORDER_TYPE_FK = 1
      where L.LOCATION_LEVEL_FK = 6
        -- Exclude FRA harbours, already in database
        AND L.LABEL NOT IN ('XGV', 'CGV', 'ESB', 'HSB', 'GMX', 'MMX', 'XBL', 'XDZ', 'LCN')
        --AND OI.RANK >= 122 /*St Malo*/
        --and OI.RANK <= 509 /*St Jean de Luz*/
        AND L.STATUS_FK IN (1, 2)
)
where LABEL NOT IN (
    'FR2GX', -- Duplicate code for Barfleur (FRBU5)
    'FRCRT', -- Barneville-Carteret (FRBNV)
    'FRGME', -- Gouville-sur-Mer (FRF2N)
    'FRV35', -- Cancale (FRCKY)
    'FRGN2', -- Saint-Guénolé (FRGN3)
    'FRTN2' /*La Trinité-sur-Mer (FRMHB)*/,
                   'FRGC2', 'FRLPE', 'FRMM8', 'FRLC6', 'FRPPN', 'FR532'
    )
    order by LABEL;
```

- Insert `ORDER_ITEM`

```sql
select
        'insert into ORDER_ITEM (ID, ORDER_TYPE_FK, OBJECT_ID, RANK, UPDATE_DATE)'
        || ' values (nextval(''order_item_seq''), 1, '
        || '(SELECT ID FROM LOCATION WHERE COMMENTS=''' || COMMENTS || ''' AND LOCATION_LEVEL_FK=2), '
        || RANK || ', current_timestamp);'
from (
    select distinct
        L.LABEL as COMMENTS,
        OI.RANK as RANK
    from LOCATION L
          inner join LOCATION_HIERARCHY LH on LH.CHILD_LOCATION_FK = L.ID
          inner join LOCATION L_COUNTRY on L_COUNTRY.ID = LH.PARENT_LOCATION_FK
             and L_COUNTRY.LOCATION_LEVEL_FK = 21 and L_COUNTRY.LABEL = 'FRA'
          inner join ORDER_ITEM OI on OI.OBJECT_ID = L.ID and OI.ORDER_TYPE_FK = 1
     where L.LOCATION_LEVEL_FK = 6
           --AND OI.RANK >= 122 /*St Malo*/
           --and OI.RANK <= 509 /*St Jean de Luz*/
           AND L.STATUS_FK IN (1, 2)
    order by RANK
);
```