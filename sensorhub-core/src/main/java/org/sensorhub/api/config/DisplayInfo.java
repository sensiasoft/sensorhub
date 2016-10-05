/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.config;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import org.sensorhub.api.comm.ICommNetwork.NetworkType;
import org.sensorhub.api.module.IModule;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Documented
public @interface DisplayInfo
{
    public String label() default "";
    public String desc() default "";
    
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface IdField
    {
        public String value() default "id";    
    }
    
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Required {}
    
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface TextInfo
    {
        public int minSize() default 1;
        public int maxSize() default 100;
        public int numLines() default 3;
        public String regex() default "";
    }
    
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ValueRange
    {
        public int min() default Integer.MIN_VALUE;
        public int max() default Integer.MAX_VALUE;
    }
    
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface FieldType
    {
        public Type value() default Type.LABEL;
        
        public enum Type
        {
            /**
             * Single line text.<br/>
             * You can restrict the possible values using {@link TextInfo}
             */
            LABEL,
            
            /**
             * Multi-line text.<br/>
             * You can restrict the possible values using {@link TextInfo}
             */
            TEXT,
            
            /**
             * Password (usually masked on UI)
             */
            PASSWORD,
            
            /**
             * Reference to another module using its local ID.<br/>
             * You can restrict the type of module using {@link ModuleType}
             */
            MODULE_ID,
            
            /**
             * Local address (e.g. address of local network interface).<br/>
             * You can restrict the address type using {@link AddressType}
             */
            LOCAL_ADDRESS,
            
            /**
             * Remote address or host name.<br/> 
             * You can restrict the address type using {@link AddressType}
             */
            REMOTE_ADDRESS,
            
            /**
             * Path on the local file system
             */
            FILESYSTEM_PATH,
            
            /**
             * To render the field as table of items.<br/>
             * The field must be a map or collection
             */
            TABLE
        }
    }
    
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ModuleType
    {
        @SuppressWarnings("rawtypes")
        public Class<? extends IModule> value() default IModule.class;
    }
    
    
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface AddressType
    {
        public NetworkType value() default NetworkType.IP;
    }
}
