---
-- #%L
-- SUMARiS:: Extraction
-- %%
-- Copyright (C) 2018 - 2023 SUMARiS Consortium
-- %%
-- This program is free software: you can redistribute it and/or modify
-- it under the terms of the GNU General Public License as
-- published by the Free Software Foundation, either version 3 of the
-- License, or (at your option) any later version.
-- 
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- GNU General Public License for more details.
-- 
-- You should have received a copy of the GNU General Public
-- License along with this program.  If not, see
-- <http://www.gnu.org/licenses/gpl-3.0.html>.
-- #L%
---

with
    strat(id, code, eotp) as (select id, name, analytic_reference from strategy where program_fk = 'SIH-PARAM-BIO'),
    labo(strat_id, labo_cd, labo) as (SELECT s.strategy_fk, listagg(d.code,',') within group (order by d.code), listagg(d.name,',') within group (order by d.code)
                                      from strategy2department s, department d, strat
                                      where s.strategy_fk = strat.id and d.id = s.department_fk  group by  s.strategy_fk),
    zone_peche(strat_id, zone_cd, zone) AS (SELECT strat.id, listagg(l.label,',') within group (order by l.label),  listagg(l.name,',') within group (order by l.label)
                                            from applied_strategy s, location l, strat
                                            where s.strategy_fk = strat.id and l.id = s.location_fk  group by strat.id),
    taxon_ref(strat_id, espece, espece_id) AS (SELECT strat.id, t.name, t.reference_taxon_fk from reference_taxon_strategy s, taxon_name t, strat
                                               where s.strategy_fk = strat.id and s.reference_taxon_fk = t.reference_taxon_fk and t.is_referent = 1
    ),
    effort_t1(strat_id, T1) AS (select strat.id, acquisition_number from applied_period ap, applied_strategy s, strat
                                where s.strategy_fk = strat.id and s.id = ap.applied_strategy_fk and to_char(start_date,'DD/MM') = '01/01'),
    effort_t1_rea(strat_id, T1_REA) AS (select strat.id, count(sa.id)
                                        from applied_period ap, applied_strategy s, sample sa, landing_measurement lm, landing l, operation o, strat
                                        where s.strategy_fk = strat.id and sa.program_fk = 'SIH-PARAM-BIO' and s.id = ap.applied_strategy_fk
                                          and extract(MONTH FROM ap.start_date) = 1
                                          and sa.fishing_operation_fk = o.id and o.fishing_trip_fk = l.fishing_trip_fk
                                          and lm.landing_fk = l.id and lm.pmfm_fk = 1389 and lm.alphanumerical_value = strat.code
                                          and sa.sample_date between ap.start_date and (ap.end_date + INTERVAL '1' DAY)
                                        GROUP BY strat.id
    ),
    effort_t1_rea_cam(strat_id, T1_REA) AS (select strat.id, count(SA.id)
                                            FROM applied_period ap, applied_strategy s, sample sa, SAMPLE_MEASUREMENT sm, operation o, FISHING_TRIP ft, SCIENTIFIC_CRUISE sc, strat
                                            WHERE s.strategy_fk = strat.id and sa.program_fk = 'SIH-PARAM-BIO' and s.id = ap.applied_strategy_fk
                                              and extract(MONTH FROM ap.start_date) = 1
                                              and ft.SCIENTIFIC_CRUISE_FK = sc.id and o.FISHING_TRIP_FK=ft.id and o.id=sa.FISHING_OPERATION_FK
                                              AND sm.PMFM_FK = 2792 AND sa.ID = sm.SAMPLE_FK AND sm.ALPHANUMERICAL_VALUE LIKE (strat.code || '%')
                                              AND sa.sample_date between ap.start_date and (ap.end_date + INTERVAL '1' DAY)
                                            GROUP BY strat.id
    ),
    effort_t2(strat_id, T2) AS (select strat.id, acquisition_number from applied_period ap, applied_strategy s, strat
                                where s.strategy_fk = strat.id and s.id = ap.applied_strategy_fk and to_char(start_date,'DD/MM') = '01/04'),
    effort_t2_rea(strat_id, T2_REA) AS (select strat.id, count(sa.id)
                                        from applied_period ap, applied_strategy s, sample sa, landing_measurement lm, landing l, operation o, strat
                                        WHERE s.strategy_fk = strat.id and sa.program_fk = 'SIH-PARAM-BIO' and s.id = ap.applied_strategy_fk
                                          and extract(MONTH FROM ap.start_date) = 4
                                          and sa.fishing_operation_fk = o.id and o.fishing_trip_fk = l.fishing_trip_fk
                                          AND lm.landing_fk = l.id and lm.pmfm_fk = 1389 and lm.alphanumerical_value = strat.code
                                          and sa.sample_date between ap.start_date and (ap.end_date + INTERVAL '1' DAY)
                                        GROUP BY strat.id
    ),
    effort_t2_rea_cam(strat_id, T2_REA) AS (select strat.id, count(SA.id)
                                            FROM applied_period ap, applied_strategy s, sample sa, SAMPLE_MEASUREMENT sm, operation o, FISHING_TRIP ft, SCIENTIFIC_CRUISE sc, strat
                                            WHERE s.strategy_fk = strat.id and sa.program_fk = 'SIH-PARAM-BIO' and s.id = ap.applied_strategy_fk
                                              and extract(MONTH FROM ap.start_date) = 4
                                              and ft.SCIENTIFIC_CRUISE_FK = sc.id and o.FISHING_TRIP_FK=ft.id and o.id=sa.FISHING_OPERATION_FK
                                              AND sm.PMFM_FK = 2792 AND sa.ID = sm.SAMPLE_FK AND sm.ALPHANUMERICAL_VALUE LIKE (strat.code || '%')
                                              and sa.sample_date between ap.start_date and (ap.end_date + INTERVAL '1' DAY)
                                            GROUP BY strat.id
    ),
    effort_t3(strat_id, T3) AS (select strat.id, acquisition_number from applied_period ap, applied_strategy s, strat
                                where s.strategy_fk = strat.id and s.id = ap.applied_strategy_fk and to_char(start_date,'DD/MM') = '01/07'),
    effort_t3_rea(strat_id, T3_REA) AS (select strat.id, count(sa.id)
                                        from applied_period ap, applied_strategy s, sample sa, landing_measurement lm, landing l, operation o, strat
                                        where s.strategy_fk = strat.id and sa.program_fk = 'SIH-PARAM-BIO' and s.id = ap.applied_strategy_fk
                                          and extract(MONTH FROM ap.start_date) = 7
                                          and sa.fishing_operation_fk = o.id and o.fishing_trip_fk = l.fishing_trip_fk
                                          and lm.landing_fk = l.id and lm.pmfm_fk = 1389 and lm.alphanumerical_value = strat.code
                                          and sa.sample_date between ap.start_date and (ap.end_date + INTERVAL '1' DAY)
                                        GROUP BY strat.id
    ),
    effort_t3_rea_cam(strat_id, T3_REA) AS (select strat.id, count(SA.id)
                                            FROM applied_period ap, applied_strategy s, sample sa, SAMPLE_MEASUREMENT sm, operation o, FISHING_TRIP ft, SCIENTIFIC_CRUISE sc, strat
                                            WHERE s.strategy_fk = strat.id and sa.program_fk = 'SIH-PARAM-BIO' and s.id = ap.applied_strategy_fk
                                              and extract(MONTH FROM ap.start_date) = 7
                                              and ft.SCIENTIFIC_CRUISE_FK = sc.id and o.FISHING_TRIP_FK=ft.id and o.id=sa.FISHING_OPERATION_FK
                                              and sm.PMFM_FK = 2792 AND sa.ID = sm.SAMPLE_FK AND sm.ALPHANUMERICAL_VALUE LIKE (strat.code || '%')
                                            GROUP BY strat.id
    ),
    effort_t4(strat_id, T4) AS (select strat.id, acquisition_number from applied_period ap, applied_strategy s, strat
                                where s.strategy_fk = strat.id and s.id = ap.applied_strategy_fk and to_char(start_date,'DD/MM') = '01/10'),
    effort_t4_rea(strat_id, T4_REA) AS (select strat.id, count(sa.id)
                                        from applied_period ap, applied_strategy s, sample sa, landing_measurement lm, landing l,
                                             operation o, strat
                                        WHERE s.strategy_fk = strat.id and sa.program_fk = 'SIH-PARAM-BIO' and s.id = ap.applied_strategy_fk
                                          and extract(MONTH FROM ap.start_date) = 10
                                          and sa.fishing_operation_fk = o.id and o.fishing_trip_fk = l.fishing_trip_fk
                                          and lm.landing_fk = l.id and lm.pmfm_fk = 1389 and lm.alphanumerical_value = strat.code
                                          and sa.sample_date between ap.start_date and (ap.end_date + INTERVAL '1' DAY)
                                        GROUP BY strat.id
    ),
    effort_t4_rea_cam(strat_id, T4_REA) AS (select strat.id, count(SA.id)
                                            FROM applied_period ap, applied_strategy s, sample sa, SAMPLE_MEASUREMENT sm, operation o, FISHING_TRIP ft, SCIENTIFIC_CRUISE sc, strat
                                            WHERE s.strategy_fk = strat.id and sa.program_fk = 'SIH-PARAM-BIO' and s.id = ap.applied_strategy_fk
                                              and extract(MONTH FROM ap.start_date) = 10
                                              and ft.SCIENTIFIC_CRUISE_FK = sc.id and o.FISHING_TRIP_FK=ft.id and o.id=sa.FISHING_OPERATION_FK
                                              and sm.PMFM_FK = 2792 AND SA.ID = sm.SAMPLE_FK AND sm.ALPHANUMERICAL_VALUE LIKE (strat.code || '%')
                                              and sa.sample_date between ap.start_date and (ap.end_date + INTERVAL '1' DAY)
                                            GROUP BY strat.id
    ),
    psfm_taille(strat_id, taille, taille_id) AS (select strat.id, listagg('['||pa.code||'] '||pa.name||' - '||u.symbol||' - '||ma.name||' - '||f.name||' - '|| m.name,',') within group (order by p.parameter_fk)  , listagg(p.id,',') within group (order by p.parameter_fk)
                                                 from pmfm_strategy s, pmfm p, method m, parameter pa, matrix ma, fraction f, unit u, strat
                                                 where s.strategy_fk = strat.id and p.id = s.pmfm_fk and s.pmfm_fk is not null and s.acquisition_level_fk = 'SAMPLE' and p.parameter_fk = pa.code and p.method_fk = m.id and p.matrix_fk = ma.id and p.fraction_fk = f.id and p.unit_fk = u.id
                                                   and pa.code like '%LENGTH%' group by strat.id),
    psfm_poids(strat_id, poids, poids_id) AS (select strat.id, listagg('['||pa.code||'] '||pa.name||' - '||u.symbol||' - '||ma.name||' - '||f.name||' - '|| m.name,',') within group (order by p.parameter_fk)  , listagg(p.id,',') within group (order by p.parameter_fk)
                                              from pmfm_strategy s, pmfm p, method m, parameter pa, matrix ma, fraction f, unit u, strat
                                              where s.strategy_fk = strat.id and p.id = s.pmfm_fk and s.pmfm_fk is not null and s.acquisition_level_fk = 'SAMPLE' and p.parameter_fk = pa.code and p.method_fk = m.id and p.matrix_fk = ma.id and p.fraction_fk = f.id and p.unit_fk = u.id
                                                and pa.code like '%WEIGHT%' group by strat.id
    ),
    psfm_sexe(strat_id, sexe, sexe_id) AS (select strat.id, listagg('['||pa.code||'] '||pa.name||' - '||u.symbol||' - '||ma.name||' - '||f.name||' - '|| m.name,',') within group (order by p.parameter_fk)  , listagg(p.id,',') within group (order by p.parameter_fk)
                                           from pmfm_strategy s, pmfm p, method m, parameter pa, matrix ma, fraction f, unit u, strat
                                           where s.strategy_fk = strat.id and p.id = s.pmfm_fk and s.pmfm_fk is not null and s.acquisition_level_fk = 'SAMPLE' and p.parameter_fk = pa.code and p.method_fk = m.id and p.matrix_fk = ma.id and p.fraction_fk = f.id and p.unit_fk = u.id
                                             and pa.code = 'SEX' group by strat.id
    ),
    psfm_age(strat_id, age, age_id) AS (select strat.id, listagg('['||pa.code||'] '||pa.name||' - '||u.symbol||' - '||ma.name||' - '||f.name||' - '|| m.name,',') within group (order by p.parameter_fk)  , listagg(p.id,' ,') within group (order by p.parameter_fk)
                                        from pmfm_strategy s, pmfm p, method m, parameter pa, matrix ma, fraction f, unit u, strat
                                        where s.strategy_fk = strat.id and p.id = s.pmfm_fk and s.pmfm_fk is not null and s.acquisition_level_fk = 'SAMPLE' and p.parameter_fk = pa.code and p.method_fk = m.id and p.matrix_fk = ma.id and p.fraction_fk = f.id and p.unit_fk = u.id
                                          and pa.code = 'AGE' group by strat.id
    ),
    psfm_maturite(strat_id, maturite, maturite_id) AS (select strat.id, listagg('['||pa.code||'] '||pa.name||' - '||u.symbol||' - '||ma.name||' - '||f.name||' - '|| m.name,',') within group (order by p.parameter_fk)  , listagg(p.id,',') within group (order by p.parameter_fk)
                                                       from pmfm_strategy s, pmfm p, method m, parameter pa, matrix ma, fraction f, unit u, strat
                                                       where s.strategy_fk = strat.id and p.id = s.pmfm_fk and s.pmfm_fk is not null and s.acquisition_level_fk = 'SAMPLE' and p.parameter_fk = pa.code and p.method_fk = m.id and p.matrix_fk = ma.id and p.fraction_fk = f.id and p.unit_fk = u.id
                                                         and pa.code like '%MATURITY%' group by strat.id
    ),
    piece_cal(strat_id, type_pc, type_pc_id)  as (SELECT strat.id, listagg(f.name,',') within group (order by f.name) , listagg(f.id,',') within group (order by f.name)
                                                  from pmfm_strategy s, fraction f, strat
                                                  where s.strategy_fk = strat.id and f.id = s.fraction_fk and s.fraction_fk is not null and s.acquisition_level_fk = 'SAMPLE' group by strat.id
    )
select id, code, eotp, labo.labo_cd, labo.labo, zone_cd, ZONE, espece, espece_id,
       taille, taille_id, poids, poids_id, sexe, sexe_id, age, age_id, maturite, maturite_id, type_pc, type_pc_id,
       T1, NVL(effort_t1_rea.T1_REA, effort_t1_rea_cam.T1_REA) T1_REA, T2, NVL(effort_t2_rea.T2_REA,effort_t2_rea_cam.T2_REA) T2_REA, T3,
       NVL(effort_t3_rea.T3_REA, effort_t3_rea_cam.T3_REA) T3_REA, T4, NVL(effort_t4_rea.T4_REA,effort_t4_rea_cam.T4_REA) T4_REA
from strat
         LEFT outer join labo on labo.strat_id = strat.id
         LEFT outer JOIN zone_peche ON zone_peche.strat_id = strat.id
         LEFT OUTER JOIN taxon_ref ON taxon_ref.strat_id = strat.id
         LEFT OUTER JOIN effort_t1 ON effort_t1.strat_id = strat.id
         LEFT OUTER JOIN effort_t1_rea ON effort_t1_rea.strat_id = strat.id
         LEFT OUTER JOIN effort_t1_rea_cam ON effort_t1_rea_cam.strat_id = strat.id
         LEFT OUTER JOIN effort_t2 ON effort_t2.strat_id = strat.id
         LEFT OUTER JOIN effort_t2_rea ON effort_t2_rea.strat_id = strat.id
         LEFT OUTER JOIN effort_t2_rea_cam ON effort_t2_rea_cam.strat_id = strat.id
         LEFT OUTER JOIN effort_t3 ON effort_t3.strat_id = strat.id
         LEFT OUTER JOIN effort_t3_rea ON effort_t3_rea.strat_id = strat.id
         LEFT OUTER JOIN effort_t3_rea_cam ON effort_t3_rea_cam.strat_id = strat.id
         LEFT OUTER JOIN effort_t4 ON effort_t4.strat_id = strat.id
         LEFT OUTER JOIN effort_t4_rea ON effort_t4_rea.strat_id = strat.id
         LEFT OUTER JOIN effort_t4_rea_cam ON effort_t4_rea_cam.strat_id = strat.id
         LEFT OUTER JOIN psfm_taille ON psfm_taille.strat_id = strat.id
         LEFT OUTER JOIN psfm_poids ON psfm_poids.strat_id = strat.id
         LEFT OUTER JOIN psfm_sexe ON psfm_sexe.strat_id = strat.id
         LEFT OUTER JOIN psfm_age ON psfm_age.strat_id = strat.id
         LEFT OUTER JOIN psfm_maturite ON psfm_maturite.strat_id = strat.id
         LEFT OUTER JOIN piece_cal ON piece_cal.strat_id = strat.id
--where strat.code='22CHITCOR001'
;

select * from SIH2_ADAGIO_DBA.SAMPLE where FISHING_OPERATION_FK;
select * from SIH2_ADAGIO_DBA.LANDING where FISHING_TRIP_FK;
select * from SIH2_ADAGIO_DBA.OPERATION where id=158500895
