package net.sumaris.core.model.referential.taxon;

import net.sumaris.core.model.referential.IItemReferentialEntity;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentityGenerator;

import java.io.Serializable;

//https://stackoverflow.com/questions/3194721/bypass-generatedvalue-in-hibernate-merge-data-not-in-db
public class UseExistingIdOrGenerate extends IdentityGenerator {

    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object obj) throws HibernateException {
        if (obj == null) throw new HibernateException(new NullPointerException()) ;

        if ((((IItemReferentialEntity) obj).getId()) == null) {
            Serializable id = super.generate(session, obj) ;
            return id;
        } else {
            return ((IItemReferentialEntity) obj).getId();

        }
    }
}
