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

package net.sumaris.server.http.rest;

import net.sumaris.server.service.administration.AccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

@RestController
public class AccountRestController {

    /* Logger */
    //private static final Logger log = LoggerFactory.getLogger(AccountRestController.class);

    @Autowired
    private AccountService accountService;

    @GetMapping(value = RestPaths.REGISTER_CONFIRM_PATH,
            produces = {
                MediaType.APPLICATION_JSON_VALUE,
                MediaType.APPLICATION_JSON_UTF8_VALUE
    })
    public boolean confirmRegistration(HttpServletResponse httpServletResponse,
                                                      @RequestParam("email") String email,
                                                      @RequestParam("code") String signatureHash) {
        accountService.confirmEmail(email, signatureHash);
        return true;
    }

}
