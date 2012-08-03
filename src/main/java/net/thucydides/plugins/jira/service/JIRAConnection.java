package net.thucydides.plugins.jira.service;

import net.thucydides.plugins.jira.client.SOAPSession;
import net.thucydides.plugins.jira.guice.Injectors;
import thucydides.plugins.jira.soap.JiraSoapService;

import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;

public class JIRAConnection {

    private final JIRAConfiguration configuration;
    private SOAPSession soapSession;

    public JIRAConnection() {
        this(Injectors.getInjector().getInstance(JIRAConfiguration.class));
    }

    public JIRAConnection(JIRAConfiguration configuration) {
        this.configuration = configuration;
    }

    private SOAPSession getSoapSession() throws MalformedURLException, RemoteException {
        if (soapSession == null) {
            soapSession = SOAPSession.openConnectionTo(new URL(getJiraWebserviceUrl()))
                    .usingCredentials(getJiraUser(), getJiraPassword());
        }
        return soapSession;
    }
    public JiraSoapService getJiraSoapService() throws MalformedURLException, RemoteException {
        return getSoapSession().getJiraSoapService();
    }

    protected JIRAConfiguration getConfiguration() {
        return configuration;
    }

    public String getJiraUser() {
        return getConfiguration().getJiraUser();
    }

    public String getJiraPassword() {
        return getConfiguration().getJiraPassword();
    }

    public String getJiraWebserviceUrl() {
        return getConfiguration().getJiraWebserviceUrl();
    }

    public String getAuthenticationToken() throws MalformedURLException, RemoteException {
        return getSoapSession().getAuthenticationToken();
    }

    public void logout() {
        this.soapSession = null;
    }
}
