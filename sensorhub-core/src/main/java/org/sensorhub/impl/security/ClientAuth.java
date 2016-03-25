/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2016 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.security.KeyStore;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStore.PasswordProtection;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;


/**
 * <p>
 * Authenticator used by HTTP clients
 * </p>
 *
 * @author Alex Robin <alex.robin@sensiasoftware.com>
 * @since Mar 24, 2016
 */
public class ClientAuth extends Authenticator
{
    static ClientAuth instance;
    
    ThreadLocal<String> currentUser = new ThreadLocal<String>();
    ThreadLocal<char[]> currentPasswd = new ThreadLocal<char[]>();
    String keyStorePath;
    KeyStore keyStore;
    char[] keyStorePasswd = null;
    SecretKeyFactory skFactory;
        
    
    public static ClientAuth getInstance()
    {
        if (instance == null)
            throw new RuntimeException("No singleton instance has been created yet");
        
        return instance;
    }
    
    
    public static void createInstance(String keystorePath)
    {
        if (instance != null)
            throw new RuntimeException("The singleton instance has already been created");
        
        instance = new ClientAuth(keystorePath);
        Authenticator.setDefault(instance);
    }
    
    
    private ClientAuth(String keystorePath)
    {
        try
        {
            // init key store
            this.keyStorePath = keystorePath;
            keyStore = KeyStore.getInstance("JCEKS");
            
            // create or open existing key store
            FileInputStream is = null;
            if (new File(keyStorePath).exists())
                is = new FileInputStream(keyStorePath);
            keyStore.load(is, keyStorePasswd);
            
            skFactory = SecretKeyFactory.getInstance("PBE");
        }
        catch (Exception e)
        {
            throw new RuntimeException("Cannot initialize key store", e);
        }
    }
    
    
    private String getKeyAlias(String host, int port, String user)
    {
        return host + ":" + port;
    }
    
    
    public void registerCredentials(String host, int port, String user, char[] passwd)
    {
        PBEKeySpec pwdSpec = new PBEKeySpec(passwd);
        
        try
        {
            // add password to key store
            String alias = getKeyAlias(host, port, user);
            SecretKey generatedSecret = skFactory.generateSecret(pwdSpec);
            PasswordProtection keyStorePP = new PasswordProtection(keyStorePasswd);
            keyStore.setEntry(alias, new SecretKeyEntry(generatedSecret), keyStorePP);
            
            // save keystore to file
            keyStore.store(new FileOutputStream(keyStorePath), keyStorePasswd);
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        finally
        {
            pwdSpec.clearPassword();
        }
    }
    
    
    public void setUser(String user)
    {
        currentUser.set(user);
    }
    
    
    public void setPassword(char[] passwd)
    {
        currentPasswd.set(passwd);
    }
    
    
    @Override
    protected PasswordAuthentication getPasswordAuthentication()
    {
        try
        {
            String user = currentUser.get();
            char[] passwd = currentPasswd.get();
            
            if (user == null)
                return super.getPasswordAuthentication();
            
            if (passwd == null)
            {
                String alias = getKeyAlias(getRequestingHost(), getRequestingPort(), user);
                PasswordProtection keyStorePP = new PasswordProtection(keyStorePasswd);
                SecretKeyEntry ske = (SecretKeyEntry)keyStore.getEntry(alias, keyStorePP);
                PBEKeySpec keySpec = (PBEKeySpec)skFactory.getKeySpec(ske.getSecretKey(), PBEKeySpec.class);
                passwd = keySpec.getPassword();
                keySpec.clearPassword();
            }
            
            // should we clear password in ThreadLocal?
            // cons = it forces us to reset credentials everytime a request is made
            return new PasswordAuthentication(user, passwd);
        }
        catch (Exception e)
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
    
    
    public void deleteKeyStore()
    {
        
    }
}
