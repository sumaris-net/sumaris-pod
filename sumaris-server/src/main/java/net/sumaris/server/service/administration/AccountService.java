package net.sumaris.server.service.administration;

/*-
 * #%L
 * SUMARiS:: Server
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

import net.sumaris.core.vo.administration.user.AccountVO;
import net.sumaris.core.vo.administration.user.PersonVO;
import net.sumaris.server.exception.InvalidEmailConfirmationException;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
public interface AccountService {


    @Transactional(readOnly = true)
    AccountVO getById(int id);

    @Transactional(readOnly = true)
    AccountVO getByPubkey(String pubkey);

    AccountVO saveAccount(AccountVO account);

    AccountVO createAccount(AccountVO account);

    AccountVO updateAccount(AccountVO account);

    void changePasswordByAccountId(Integer idAccount, String pubkey);

    void confirmEmail(String email, String signatureHash) throws InvalidEmailConfirmationException;

    void confirmChangePassword(String token, String toAddress, String pubkey);

    void sendChangePassword(String email, String locale);

    void sendConfirmationEmail(String email, String locale) throws InvalidEmailConfirmationException;


    @Transactional(readOnly = true)
    List<String> getAllTokensByPubkey(String pubkey);

    @Transactional(readOnly = false)
    List<String> deleteAllTokensByPubkey(String pubkey);

    @Transactional(readOnly = true)
    boolean isStoredToken(String token, String pubkey);

    void addToken(String token, String pubkey);

    @Transactional(readOnly = true)
    AccountVO toAccountVO(PersonVO person);

}
