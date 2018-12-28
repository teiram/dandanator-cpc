package com.grelobites.romgenerator.jumpblock;

import com.grelobites.romgenerator.Constants;
import com.grelobites.romgenerator.model.SnapshotGame;
import com.grelobites.romgenerator.util.Util;
import com.grelobites.romgenerator.util.gameloader.loaders.SNAGameImageLoader;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;

public class JumpBlockTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(JumpBlockTests.class);

    private static final int toInteger(String value) {
        return Integer.parseInt(value, 16);
    }

    @Test
    public void test1942JumpBlock() throws IOException, SQLException {
        //final String snaLocation = "/sna/1942-6128.sna";
        final String snaLocation = "/sna/6128loader.sna";
        SnapshotGame game = (SnapshotGame) new SNAGameImageLoader().load(JumpBlockTests.class
                .getResourceAsStream(snaLocation));
        Connection connection = DriverManager.getConnection("jdbc:sqlite:src/test/resources/db/cpc-jumpblock.db");
        try {
            PreparedStatement stmt = connection.prepareStatement("select addr, cpc464, cpc664, cpc6128 from jumps");
            ResultSet rs = stmt.executeQuery();
            int matches464 = 0;
            int matches664 = 0;
            int matches6128 = 0;
            int missed = 0;
            int total = 0;
            while (rs.next()) {
                int addr = toInteger(rs.getString(1));
                int cpc464Addr = toInteger(rs.getString(2));
                int cpc664Addr = toInteger(rs.getString(3));
                int cpc6128Addr = toInteger(rs.getString(4));
                byte[] slot = game.getSlot(addr / Constants.SLOT_SIZE);
                int value = Util.readAsLittleEndian(slot, addr % Constants.SLOT_SIZE + 1);
                int cpc464delta = value == cpc464Addr ? 1 : 0;
                int cpc664delta = value == cpc664Addr ? 1 : 0;
                int cpc6128delta = value == cpc6128Addr ? 1 : 0;
                matches464 += cpc464delta;
                matches664 += cpc664delta;
                matches6128 += cpc6128delta;
                missed += cpc464delta + cpc664delta + cpc6128delta > 0 ? 0 : 1;
                total++;
                if ((value & 0x7FFF) != (cpc6128Addr & 0x7fff)) {
                    LOGGER.debug("Non matching {} != {} in addr {}",
                            String.format("0x%04x", value),
                            String.format("0x%04x", cpc6128Addr),
                            String.format("0x%04x", addr));
                }
            }
            LOGGER.debug("Total: {}. Matched 464: {}. Matched 664: {}. Matched 6128: {}. Misssed: {}",
                    total, matches464, matches664, matches6128, missed);
        } finally {
            connection.close();
        }
    }
}
