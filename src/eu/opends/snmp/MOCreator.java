/**
 * Created by Gustavo on 12/10/2016.
 */

package eu.opends.snmp;

import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOScalar;
import org.snmp4j.smi.*;

/**
 * This class creates and returns ManagedObjects
 * @author Shiva
 *
 */
public class MOCreator {
    public static MOScalar createReadOnly(OID oid,Object value ){
        return new MOScalar(oid,
                MOAccessImpl.ACCESS_READ_ONLY,
                getVariable(value));
    }

    private static Variable getVariable(Object value) {
        if(value instanceof String) {
            return new OctetString((String)value);
        }
        else if(value instanceof Float) {
            return new Gauge32(((Float)value).longValue());
        }
        else if (value instanceof Long) {
            return new Gauge32((Long) value);
        }
        else if (value instanceof Integer) {
            return new Integer32((Integer) value);
        }
        throw new IllegalArgumentException("Unmanaged Type: " + value.getClass());
    }

}
