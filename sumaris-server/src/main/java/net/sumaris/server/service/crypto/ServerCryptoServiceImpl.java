package net.sumaris.server.service.crypto;

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

import lombok.extern.slf4j.Slf4j;
import net.sumaris.core.service.crypto.CryptoService;
import net.sumaris.core.util.crypto.CryptoUtils;
import net.sumaris.core.util.crypto.KeyPair;
import net.sumaris.server.config.SumarisServerConfiguration;
import org.nuiton.i18n.I18n;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service("serverCryptoService")
@Slf4j
public class ServerCryptoServiceImpl extends net.sumaris.core.service.crypto.CryptoServiceImpl implements ServerCryptoService {

    private final SumarisServerConfiguration config;

    private final CryptoService cryptoService;

    private final KeyPair serverKeyPair;

    private final String serverPubkey;

    @Autowired
    public ServerCryptoServiceImpl(SumarisServerConfiguration config, CryptoService cryptoService) {
        this.cryptoService = cryptoService;
        this.config = config;

        // Generate server keypair
        if (StringUtils.isEmpty(config.getKeypairSalt()) || StringUtils.isEmpty(config.getKeypairSalt())) {
            this.serverKeyPair = cryptoService.getRandomKeypair();
            this.serverPubkey = CryptoUtils.encodeBase58(this.serverKeyPair.getPubKey());
            log.warn(I18n.t("sumaris.server.keypair.pubkey.random", serverPubkey));
        }
        else {
            this.serverKeyPair = cryptoService.getKeyPair(config.getKeypairSalt(), config.getKeypairPassword());
            this.serverPubkey = CryptoUtils.encodeBase58(this.serverKeyPair.getPubKey());
            log.warn(I18n.t("sumaris.server.keypair.pubkey", serverPubkey));
        }
    }

    @Override
    public KeyPair getServerKeypair() {
        return this.serverKeyPair;
    }

    @Override
    public String getServerPubkey() {
        return this.serverPubkey;
    }

    @Override
    public String sign(String message) {
        return this.sign(message, serverKeyPair.secretKey);
    }
}
