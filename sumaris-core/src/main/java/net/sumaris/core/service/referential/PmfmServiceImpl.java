package net.sumaris.core.service.referential;

/*-
 * #%L
 * SUMARiS:: Core
 * %%
 * Copyright (C) 2018 SUMARiS Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */


import com.google.common.base.Preconditions;
import net.sumaris.core.dao.referential.PmfmDao;
import net.sumaris.core.vo.referential.PmfmVO;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("pmfmService")
public class PmfmServiceImpl implements PmfmService {

	private static final Log log = LogFactory.getLog(PmfmServiceImpl.class);

	@Autowired
	protected PmfmDao pmfmDao;

	@Override
	public PmfmVO getByLabel(final String label) {
		Preconditions.checkNotNull(label);
		Preconditions.checkArgument(label.trim().length() > 0);
		return pmfmDao.getByLabel(label.trim());
	}

	@Override
	public PmfmVO get(int pmfmId) {
		return pmfmDao.get(pmfmId);
	}

	@Override
	public boolean isWeightPmfm(int pmfmId) {
		PmfmVO pmfm = pmfmDao.get(pmfmId);
		return pmfm.getLabel() != null && pmfm.getLabel().endsWith("WEIGHT");
	}
}
