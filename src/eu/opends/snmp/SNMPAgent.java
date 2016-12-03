/**
 * Created by Gustavo on 12/10/2016.
 */

package eu.opends.snmp;



import eu.opends.main.Simulator;
import eu.opends.knowledgeBase.KnowledgeBase;

import java.io.DataInputStream;

import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.snmp4j.TransportMapping;
import org.snmp4j.agent.BaseAgent;
import org.snmp4j.agent.CommandProcessor;
import org.snmp4j.agent.DuplicateRegistrationException;
import org.snmp4j.agent.MOGroup;
import org.snmp4j.agent.ManagedObject;
import org.snmp4j.agent.mo.*;
import org.snmp4j.agent.mo.snmp.RowStatus;
import org.snmp4j.agent.mo.snmp.SnmpCommunityMIB;
import org.snmp4j.agent.mo.snmp.SnmpNotificationMIB;
import org.snmp4j.agent.mo.snmp.SnmpTargetMIB;
import org.snmp4j.agent.mo.snmp.StorageType;
import org.snmp4j.agent.mo.snmp.VacmMIB;
import org.snmp4j.agent.security.MutableVACM;
import org.snmp4j.mp.MPv3;
import org.snmp4j.security.SecurityLevel;
import org.snmp4j.security.SecurityModel;
import org.snmp4j.security.USM;
import org.snmp4j.smi.*;
import org.snmp4j.transport.TransportMappings;

import static org.snmp4j.agent.mo.snmp.NotificationLogMib.NlmLogVariableValueTypeEnum.integer32;

public class SNMPAgent extends BaseAgent {

    private String address;
    private boolean mibInitialized = false;

    /**
     *
     * @param address
     * @throws IOException
     */

    private Simulator sim;

    private MOScalar evBrandModel, evVIN,  evMaxPower, evSpeed, evLocation, evKM;
    private MOScalar motorDescription, motorRPM, motorMaxPower;
    private MOScalar btryCapacity, btryChargeState, btryModuleCount, btryVoltage, btryCurrent;
    private MOTable btryModulesTable;

    public SNMPAgent(String address, Simulator sim) throws IOException {

        /**
         * Creates a base agent with boot-counter, config file, and a
         * CommandProcessor for processing SNMP requests. Parameters:
         * "bootCounterFile" - a file with serialized boot-counter information
         * (read/write). If the file does not exist it is created on shutdown of
         * the agent. "configFile" - a file with serialized configuration
         * information (read/write). If the file does not exist it is created on
         * shutdown of the agent. "commandProcessor" - the CommandProcessor
         * instance that handles the SNMP requests.
         */
        super(new File("conf.agent"), new File("bootCounter.agent"),
                new CommandProcessor(
                        new OctetString(MPv3.createLocalEngineID())));
        this.address = address;
        this.sim = sim;
    }

    /**
     * Adds community to security name mappings needed for SNMPv1 and SNMPv2c.
     */
    @Override
    protected void addCommunities(SnmpCommunityMIB communityMIB) {
        Variable[] com2sec = new Variable[]{new OctetString("public"), new OctetString("cpublic"), this.getAgent().getContextEngineID(), new OctetString("public"), new OctetString(), new Integer32(3), new Integer32(1)};
        SnmpCommunityMIB.SnmpCommunityEntryRow row = communityMIB.getSnmpCommunityEntry().createRow((new OctetString("public2public")).toSubIndex(true), com2sec);
        communityMIB.getSnmpCommunityEntry().addRow(row);
    }

    /**
     * Adds initial notification targets and filters.
     */
    @Override
    protected void addNotificationTargets(SnmpTargetMIB arg0,
                                          SnmpNotificationMIB arg1) {
        // TODO Auto-generated method stub

    }

    /**
     * Adds all the necessary initial users to the USM.
     */
    @Override
    protected void addUsmUser(USM arg0) {
        // TODO Auto-generated method stub

    }

    /**
     * Adds initial VACM configuration.
     */
    @Override
    protected void addViews(VacmMIB vacm) {
        vacm.addGroup(SecurityModel.SECURITY_MODEL_SNMPv2c, new OctetString(
                        "cpublic"), new OctetString("v1v2group"),
                StorageType.nonVolatile);

        vacm.addAccess(new OctetString("v1v2group"), new OctetString("public"),
                SecurityModel.SECURITY_MODEL_ANY, SecurityLevel.NOAUTH_NOPRIV,
                MutableVACM.VACM_MATCH_EXACT, new OctetString("fullReadView"),
                new OctetString("fullWriteView"), new OctetString(
                        "fullNotifyView"), StorageType.nonVolatile);

        vacm.addViewTreeFamily(new OctetString("fullReadView"), new OID("1.3"),
                new OctetString(), VacmMIB.vacmViewIncluded,
                StorageType.nonVolatile);

    }

    /**
     * Unregister the basic MIB modules from the agent's MOServer.
     */
    @Override
    protected void unregisterManagedObjects() {
        // TODO Auto-generated method stub

    }

    /**
     * Register additional managed objects at the agent's server.
     */
    @Override
    protected void registerManagedObjects() {
        // TODO Auto-generated method stub

    }

    protected void initTransportMappings() throws IOException {
        transportMappings = new TransportMapping[1];
        Address addr = GenericAddress.parse(address);
        TransportMapping tm = TransportMappings.getInstance()
                .createTransportMapping(addr);
        transportMappings[0] = tm;
    }

    /**
     * Start method invokes some initialization methods needed to start the
     * agent
     *
     * @throws IOException
     */
    public void start() throws IOException {

        init();
        // This method reads some old config from a file and causes
        // unexpected behavior.
        // loadConfig(ImportModes.REPLACE_CREATE);
        addShutdownHook();
        getServer().addContext(new OctetString("public"));
        finishInit();
        run();
        sendColdStartNotification();

        initializeMIB();
    }

    /**
     * Clients can register the MO they need
     */
    public void registerManagedObject(ManagedObject mo) {
        try {
            server.register(mo, null);
        } catch (DuplicateRegistrationException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void unregisterManagedObject(MOGroup moGroup) {
        moGroup.unregisterMOs(server, getContext(moGroup));
    }

    private void initializeMIB() {

        //OID sysDescr = new OID(".1.3.6.1.2.1.1.1.0");
        //this.unregisterManagedObject(this.getSnmpv2MIB());
        //this.registerManagedObject(MOCreator.createReadOnly(sysDescr,"EH NOIS Q VOA BRUSHAUM"));

        //Inicializa OIDs de acordo com a MIB
        OID evBrandModelOID = new OID(".1.3.6.1.4.1.12619.5.1.0");
        OID evVINOID = new OID(".1.3.6.1.4.1.12619.5.2.0");
        OID evMaxPowerOID = new OID(".1.3.6.1.4.1.12619.5.3.0");
        OID evSpeedOID = new OID(".1.3.6.1.4.1.12619.5.4.0");
        OID evLocationOID = new OID(".1.3.6.1.4.1.12619.5.5.0");
        OID evKMOid = new OID(".1.3.6.1.4.1.12619.5.6.0");

        OID motorDescriptionOID = new OID(".1.3.6.1.4.1.12619.5.8.1.0");
        OID motorRPMOID = new OID(".1.3.6.1.4.1.12619.5.8.2.0");
        OID motorMaxPowerOID = new OID(".1.3.6.1.4.1.12619.5.8.3.0");

        OID btryCapacityOID = new OID(".1.3.6.1.4.1.12619.5.9.1.0");
        OID btryChargeStateOID = new OID(".1.3.6.1.4.1.12619.5.9.2.0");
        OID btryModuleCountOID = new OID(".1.3.6.1.4.1.12619.5.9.3.0");
        OID btryVoltageOID = new OID(".1.3.6.1.4.1.12619.5.9.4.0");
        OID btryCurrentOID = new OID(".1.3.6.1.4.1.12619.5.9.5.0");
        OID btryModulesTableOID = new OID(".1.3.6.1.4.1.12619.5.9.6");
        OID btryModulesTableEntryOID = new OID(".1.3.6.1.4.1.12619.5.9.6.1");

        //Inicializa Objetos da MIB
        evBrandModel = MOCreator.createReadOnly(evBrandModelOID, "Tesla Model Bruxao S");
        this.registerManagedObject(evBrandModel);

        evVIN = MOCreator.createReadOnly(evVINOID, "1234567890");
        this.registerManagedObject(evVIN);

        evMaxPower = MOCreator.createReadOnly(evMaxPowerOID, "1000");
        this.registerManagedObject(evMaxPower);

        evSpeed = MOCreator.createReadOnly(evSpeedOID, 0.0f);
        this.registerManagedObject(evSpeed);

        evLocation = MOCreator.createReadOnly(evLocationOID, "(0,0)");
        this.registerManagedObject(evLocation);

        evKM = MOCreator.createReadOnly(evKMOid, "0");
        this.registerManagedObject(evKM);


        motorDescription = MOCreator.createReadOnly(motorDescriptionOID, "Sem duvida eh um motor eletrico");
        this.registerManagedObject(motorDescription);

        motorRPM = MOCreator.createReadOnly(motorRPMOID, 0.0f);
        this.registerManagedObject(motorRPM);

        motorMaxPower = MOCreator.createReadOnly(motorMaxPowerOID, 500);
        this.registerManagedObject(motorMaxPower);


        btryCapacity = MOCreator.createReadOnly(btryCapacityOID, sim.getCar().BATTERYWh);
        this.registerManagedObject(btryCapacity);
        btryCapacity.setValue(new OctetString(String.valueOf(sim.getCar().BATTERYWh)));

        btryChargeState = MOCreator.createReadOnly(btryChargeStateOID, sim.getCar().BATTERYWh);
        this.registerManagedObject(btryChargeState);

        btryVoltage = MOCreator.createReadOnly(btryVoltageOID, 0);
        this.registerManagedObject(btryVoltage);

        btryCurrent = MOCreator.createReadOnly(btryCurrentOID, 0);
        this.registerManagedObject(btryCurrent);

        btryModuleCount = MOCreator.createReadOnly(btryModuleCountOID, 16);
        this.registerManagedObject(btryModuleCount);

        MOTableBuilder builder = new MOTableBuilder(btryModulesTableEntryOID)
                .addColumnType(SMIConstants.SYNTAX_INTEGER,MOAccessImpl.ACCESS_READ_ONLY)//index
                .addColumnType(SMIConstants.SYNTAX_INTEGER,MOAccessImpl.ACCESS_READ_ONLY)//capacity
                .addColumnType(SMIConstants.SYNTAX_INTEGER,MOAccessImpl.ACCESS_READ_ONLY)//Imax
                .addColumnType(SMIConstants.SYNTAX_GAUGE32,MOAccessImpl.ACCESS_READ_ONLY)//temperature
                .addColumnType(SMIConstants.SYNTAX_GAUGE32,MOAccessImpl.ACCESS_READ_ONLY)//Voltage
                .addColumnType(SMIConstants.SYNTAX_GAUGE32,MOAccessImpl.ACCESS_READ_ONLY)//ChargeState
                .addColumnType(SMIConstants.SYNTAX_OCTET_STRING,MOAccessImpl.ACCESS_READ_ONLY);//date
                for (int i=1; i<17; i++){
                    builder
                        .addRowValue(new Integer32(i))
                        .addRowValue(new Integer32(5625))  //Wh
                        .addRowValue(new Integer32(1000))  //A
                        .addRowValue(new Gauge32(25))       //ºC
                        .addRowValue(new Gauge32(25200)) //em mV
                        .addRowValue(new Gauge32(5625)) //em Wh
                        .addRowValue(new OctetString("15/11/2015"));
                }
        btryModulesTable = builder.build();
        this.registerManagedObject(btryModulesTable);

        mibInitialized = true;
    }

    public void updateData() {

        if(!mibInitialized)
            return;

        Float fBattery = sim.getCar().getWhLeft();
        btryChargeState.setValue(new Gauge32(fBattery.longValue()));

        Float fV = sim.getCar().getVoltage();
        btryVoltage.setValue(new Gauge32(fV.longValue()));

        btryModuleCount.setValue(new Integer32(16));

        List<Variable[]> tableRows = new ArrayList<Variable[]>();
        for(int i=1; i<17; i++){
            MOTableRow row;
            OID rowOID = new OID(String.valueOf(i));
            row = btryModulesTable.removeRow(rowOID);

            int prettyRandom = 9 + (int) (Math.random() * 20);

            tableRows.add(new Variable[btryModulesTable.getColumnCount()]);
            tableRows.get(i-1)[0] = new Integer32(i);
            tableRows.get(i-1)[1] = new Integer32(5625);  //Wh
            tableRows.get(i-1)[2] = new Integer32(1000);  //A
            tableRows.get(i-1)[3] = new Gauge32(25);       //ºC
            tableRows.get(i-1)[4] = new Gauge32((long)(1000*fV/16) + prettyRandom); //em mV
            tableRows.get(i-1)[5] = new Gauge32((long)(fBattery/16)+ prettyRandom); //em Wh
            tableRows.get(i-1)[6] = new OctetString("15/11/2015");

            MOMutableTableModel model = (MOMutableTableModel) btryModulesTable.getModel();

            //model.getRow("").
            int j = 1;
            for (Variable[] variables : tableRows) {
                model.addRow(new DefaultMOMutableRow2PC(new OID(String.valueOf(j)),
                        variables));
                j++;
            }
            //btryModulesTable.addNewRow(rowOID, variables);
            //btryModulesTable.createRow(rowOID,variables);
        }

        Float fI = sim.getCar().getCurrent();
        btryCurrent.setValue(new Gauge32(fI.longValue()));

        Float fSpeed = sim.getCar().getCurrentSpeedKmh();
        evSpeed.setValue(new Gauge32(fSpeed.longValue()));

        Float fRPM = sim.getCar().getTransmission().getRPM();
        motorRPM.setValue(new Gauge32(fRPM.longValue()));

        Float fMileage = sim.getCar().getMileage() / 1000.0f;
        evKM.setValue(new OctetString(fMileage.toString()));
    }

}