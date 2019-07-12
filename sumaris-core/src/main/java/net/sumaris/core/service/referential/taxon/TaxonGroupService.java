package net.sumaris.core.service.referential.taxon;


import org.springframework.transaction.annotation.Transactional;

@Transactional
public interface TaxonGroupService {

    @Transactional
    void updateTaxonGroupHierarchies();

}
