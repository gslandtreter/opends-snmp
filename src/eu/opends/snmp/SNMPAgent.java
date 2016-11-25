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

        motorMaxPower = MOCreator.createReadOnly(motorMaxPowerOID, 1500);
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

        btryModuleCount = MOCreator.createReadOnly(btryModuleCountOID, 10);
        this.registerManagedObject(btryModuleCount);

        MOColumn colunas[];
        colunas = new MOColumn[1];
        colunas[1] = new MOColumn(1,1);
      //  OID btryModulesTableIndexOID = new OID(".1.3.6.1.4.1.12619.5.9.6.1.1");
        MOTableIndex indice = new MOTableIndex(new MOTableSubIndex[1]);
//MOTableSubIndex(btryModulesTableIndexOID,1),
        btryModulesTable =  MOCreator.createTable(btryModulesTableOID, indice, colunas);

        mibInitialized = true;
    }

    public void updateData() {

        if(!mibInitialized)
            return;
        //try {
        Float fBattery = sim.getCar().getWhLeft();
        btryChargeState.setValue(new Gauge32(fBattery.longValue()));

        Float fV = sim.getCar().getVoltage();
        btryVoltage.setValue(new Gauge32(fV.longValue()));

        Float fI = sim.getCar().getCurrent();
        btryCurrent.setValue(new Gauge32(fI.longValue()));
        //}
        //catch(Exception ex)
        //{

        //}
        Float fSpeed = sim.getCar().getCurrentSpeedKmh();
        evSpeed.setValue(new Gauge32(fSpeed.longValue()));

        Float fRPM = sim.getCar().getTransmission().getRPM();
        motorRPM.setValue(new Gauge32(fRPM.longValue()));

        Float fMileage = sim.getCar().getMileage() / 1000.0f;
        evKM.setValue(new OctetString(fMileage.toString()));


        //int iCapacity = 90000;//sim.getCar().getMileage() / 1000.0f;
        //btryCapacity.setValue(new Integer32(iCapacity));

    }

}