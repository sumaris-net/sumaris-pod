package net.sumaris.core.vo.technical.extraction;

import com.google.common.base.Preconditions;
import net.sumaris.core.model.data.IWithRecorderDepartmentEntity;
import net.sumaris.core.model.data.IWithRecorderPersonEntity;
import net.sumaris.core.util.StringUtils;
import net.sumaris.core.vo.administration.user.DepartmentVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.core.vo.referential.IReferentialVO;
import org.apache.commons.collections4.ListUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Benoit Lavenier <benoit.lavenier@e-is.pro>*
 */
@lombok.Data
public class ExtractionProductVO implements IReferentialVO,
        IWithRecorderDepartmentEntity<Integer, DepartmentVO>,
        IWithRecorderPersonEntity<Integer, PersonVO> {

    private Integer id;
    private String label;
    private String name;
    private String description;
    private String comments;
    private Date updateDate;
    private Date creationDate;
    private Boolean isSpatial;

    private DepartmentVO recorderDepartment;
    private PersonVO recorderPerson;

    private Integer statusId;
    private Integer parentId;

    private List<ExtractionProductTableVO> tables;

    public List<String> getTableNames() {
        if (tables == null) return null;
        return tables.stream().map(t -> t.getTableName()).collect(Collectors.toList());
    }

    public List<String> getSheetNames() {
        if (tables == null) return null;
        return tables.stream().map(t -> t.getLabel()).collect(Collectors.toList());
    }

    public Map<String, String> getItems() {
        if (tables == null) return null;
        return tables.stream().collect(Collectors.toMap(t -> t.getLabel(), t -> t.getTableName()));
    }

    public Optional<String> getTableNameBySheetName(String sheetName) {
        Preconditions.checkNotNull(sheetName);
        return ListUtils.emptyIfNull(tables).stream()
                .filter(t -> sheetName.equalsIgnoreCase(t.getLabel()))
                .map(t -> t.getTableName())
                .findFirst();
    }

    public Optional<String> getSheetNameByTableName(String tableName) {
        Preconditions.checkNotNull(tableName);
        return ListUtils.emptyIfNull(tables).stream()
                .filter(t -> tableName.equalsIgnoreCase(t.getTableName()))
                .map(t -> t.getLabel())
                .findFirst();
    }

    public boolean hasSpatialSheet() {
        return ListUtils.emptyIfNull(tables).stream()
                .anyMatch(t -> t.getIsSpatial() != null && t.getIsSpatial().booleanValue());

    }

}
