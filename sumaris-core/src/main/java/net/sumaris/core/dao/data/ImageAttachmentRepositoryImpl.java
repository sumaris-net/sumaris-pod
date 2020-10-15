package net.sumaris.core.dao.data;

import com.google.common.base.Preconditions;
import net.sumaris.core.model.data.ImageAttachment;
import net.sumaris.core.vo.data.DataFetchOptions;
import net.sumaris.core.vo.data.ImageAttachmentVO;
import net.sumaris.core.vo.filter.IDataFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;

/**
 * @author peck7 on 31/08/2020.
 */
public class ImageAttachmentRepositoryImpl
    extends DataRepositoryImpl<ImageAttachment, ImageAttachmentVO, IDataFilter, DataFetchOptions> {

    private static final Logger log =
        LoggerFactory.getLogger(ImageAttachmentRepositoryImpl.class);

    protected ImageAttachmentRepositoryImpl(EntityManager entityManager) {
        super(ImageAttachment.class, ImageAttachmentVO.class, entityManager);
        setLockForUpdate(true);
    }

    @Override
    public ImageAttachmentVO save(ImageAttachmentVO vo) {
        Preconditions.checkNotNull(vo);
        Preconditions.checkNotNull(vo.getContent());
        Preconditions.checkNotNull(vo.getContentType());
        Preconditions.checkNotNull(vo.getDateTime());
        Preconditions.checkNotNull(vo.getRecorderDepartment());
        Preconditions.checkNotNull(vo.getRecorderDepartment().getId());

        return super.save(vo);
    }

    @Override
    public void toEntity(ImageAttachmentVO source, ImageAttachment target, boolean copyIfNull) {
        super.toEntity(source, target, copyIfNull);
        // Recorder person
        DataDaos.copyRecorderPerson(getEntityManager(), source, target, copyIfNull);
    }

    @Override
    protected void onBeforeSaveEntity(ImageAttachmentVO vo, ImageAttachment entity, boolean isNew) {
        super.onBeforeSaveEntity(vo, entity, isNew);

        // When new entity: set the creation date
        if (isNew || entity.getCreationDate() == null) {
            entity.setCreationDate(entity.getUpdateDate());
        }
    }

    @Override
    protected void onAfterSaveEntity(ImageAttachmentVO vo, ImageAttachment savedEntity, boolean isNew) {
        super.onAfterSaveEntity(vo, savedEntity, isNew);
        if (isNew) {
            vo.setCreationDate(savedEntity.getCreationDate());
        }
    }

}
