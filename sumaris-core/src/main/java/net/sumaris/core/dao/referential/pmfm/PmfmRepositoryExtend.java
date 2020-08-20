package net.sumaris.core.dao.referential.pmfm;

/**
 * @author peck7 on 19/08/2020.
 */
public interface PmfmRepositoryExtend {

    boolean hasLabelPrefix(int pmfmId, String... labelPrefixes);

    boolean hasLabelSuffix(int pmfmId, String... labelSuffixes);

}
