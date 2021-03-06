package eu.opends.snmp;

    import java.util.ArrayList;
    import java.util.List;

    import org.snmp4j.agent.MOAccess;
    import org.snmp4j.agent.mo.DefaultMOMutableRow2PC;
    import org.snmp4j.agent.mo.DefaultMOTable;
    import org.snmp4j.agent.mo.MOColumn;
    import org.snmp4j.agent.mo.MOMutableTableModel;
    import org.snmp4j.agent.mo.MOTable;
    import org.snmp4j.agent.mo.MOTableIndex;
    import org.snmp4j.agent.mo.MOTableSubIndex;
    import org.snmp4j.smi.OID;
    import org.snmp4j.smi.SMIConstants;
    import org.snmp4j.smi.Variable;
/**
 * Created on 27/11/2016.
 */
/**
 * <p>Utility class for adding dynamic data into an {@link MOTable}</p>
 *
 *
 <pre><code>
 MOTableBuilder builder = new MOTableBuilder(new OID(".1.3.6.1.2.1.2.2.1"))
 .addColumnType(SMIConstants.SYNTAX_INTEGER,MOAccessImpl.ACCESS_READ_ONLY)
 .addColumnType(SMIConstants.SYNTAX_OCTET_STRING,MOAccessImpl.ACCESS_READ_ONLY);
 for(MyObject o: myObjects) {
 builder.addRowValue(new Integer32(o.getId()))
 .addRowValue(new OctetString(o.getName()));
 }
 MOTable table = builder.build();
 </code><pre>

 * @author johanrask
 *
 */

public class MOTableBuilder {

    private MOTableSubIndex[] subIndexes = new MOTableSubIndex[] {
            new MOTableSubIndex(new OID(".1.3.6.1.4.1.12619.5.9.6.1.1"), SMIConstants.SYNTAX_INTEGER)
    };
    //private MOTableSubIndex[] subIndexes = new MOTableSubIndex[] { new MOTableSubIndex(SMIConstants.SYNTAX_INTEGER) };
    private MOTableIndex indexDef = new MOTableIndex(subIndexes,true);//false);

    private final List<MOColumn> columns = new ArrayList<MOColumn>();
    private final List<Variable[]> tableRows = new ArrayList<Variable[]>();
    private int currentRow = 0;
    private int currentCol = 0;

    private OID tableRootOid;

    private int colTypeCnt = 0;


    /**
     * Specified oid is the root oid of this table
     */
    public MOTableBuilder(OID oid) {
        this.tableRootOid = oid;
    }

    /**
     * Adds all column types {@link MOColumn} to this table.
     * Important to understand that you must add all types here before
     * adding any row values
     *
     * @param syntax use {@link SMIConstants}
     * @param access
     * @return
     */
    public MOTableBuilder addColumnType(int syntax, MOAccess access) {
        colTypeCnt++;
        columns.add(new MOColumn(colTypeCnt, syntax, access));
        return this;
    }


    public MOTableBuilder addRowValue(Variable variable) {
        if (tableRows.size() == currentRow) {
            tableRows.add(new Variable[columns.size()]);
        }
        tableRows.get(currentRow)[currentCol] = variable;
        currentCol++;
        if (currentCol >= columns.size()) {
            currentRow++;
            currentCol = 0;
        }
        return this;
    }

    public MOTable build() {
        DefaultMOTable newTable = new DefaultMOTable(tableRootOid, indexDef,
                columns.toArray(new MOColumn[0]));
        MOMutableTableModel model = (MOMutableTableModel) newTable.getModel();
        int i = 1;

        for (Variable[] variables : tableRows) {
            model.addRow(new DefaultMOMutableRow2PC(new OID(String.valueOf(i)),
                    variables));
            i++;
        }
        newTable.setVolatile(true);
        return newTable;
    }
}