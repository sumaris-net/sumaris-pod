/*
 * #%L
 * SUMARiS
 * %%
 * Copyright (C) 2019 SUMARiS Consortium
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

package net.sumaris.core.service.referential.pmfm;

import com.google.common.base.Preconditions;
import net.sumaris.core.dao.referential.pmfm.PmfmDao;
import net.sumaris.core.vo.referential.PmfmVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service("pmfmService")
public class PmfmServiceImpl implements PmfmService {

	private static final Logger log = LoggerFactory.getLogger(PmfmServiceImpl.class);

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

	@Override
	public PmfmVO save(PmfmVO pmfm) {
		return pmfmDao.save(pmfm);
	}
}
