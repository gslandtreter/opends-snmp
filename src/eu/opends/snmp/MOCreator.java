/**
 * Created by Gustavo on 12/10/2016.
 */

package eu.opends.snmp;

import org.snmp4j.agent.MOAccess;
import org.snmp4j.agent.MOScope;
import org.snmp4j.agent.mo.*;
import org.snmp4j.agent.mo.snmp.SysUpTime;
import org.snmp4j.agent.mo.snmp.tc.TextualConvention;
import org.snmp4j.agent.request.SubRequest;
import org.snmp4j.agent.mo.MOFactory;
import org.snmp4j.smi.*;

/**
 * This class creates and returns ManagedObjects
 * @author Shiva
 *
 */
public class MOCreator {
    public  static MOScalar createReadOnly(OID oid,Object value ){
        return new MOScalar(oid,
                MOAccessImpl.ACCESS_READ_ONLY,
                getVariable(value));
    }

    public static MOTable createTable(org.snmp4j.smi.OID oid,
                                      MOTableIndex indexDef,
                                      MOColumn[] columns){
        MOCreator moCreator = new MOCreator();
        MOTable newTable = moCreator.createTable(oid, indexDef, columns);

        return newTable;
    }


    private  static Variable getVariable(Object value) {
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
