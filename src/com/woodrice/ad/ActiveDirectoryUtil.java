package com.woodrice.ad;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

/**  
 * ActiveDirectoryUtil
 * 
 * @Project: WRActiveDirectory
 * @Title: ActiveDirectoryUtil.java
 * @Package com.woodrice.ad
 * @Description: ActiveDirectory
 * @author lmyanglei@gmail.com
 * @date 2014-1-20 14:28:01
 * @Copyright: 2014 woodrice.com All rights reserved.
 * @version v1.0  
 */
public class ActiveDirectoryUtil {

	private LdapContext ldapContext = null;
	private DirContext dirContext = null;
	private String DC1 = null;
	private String DC2 = null;
	
    private String IP = null;
    private String PORT = null;
    private String adminName = null;
    private String adminPassword = null;
    private String URL = null;// 创建用户时，不需要domain信息；查询和修改密码时，需要domain信息

    private String replaceSuffix = null;// 获取DN时，会带有“,DC=domain,DC=com”字样，在进行操作时，需要去掉
	private String domainSuffix = null;
	private String mailSuffix = null;
    
	private int UF_ACCOUNTDISABLE = 0x0002;   
	private int UF_PASSWD_NOTREQD = 0x0020;   
	private int UF_PASSWD_CANT_CHANGE = 0x0040;   
	private int UF_NORMAL_ACCOUNT = 0x0200;   
	private int UF_DONT_EXPIRE_PASSWD = 0x10000;   
	private int UF_PASSWORD_EXPIRED = 0x800000;   

	/**
	 * 查找用户的
	 * 
	 * distinguishedName
	 * 
	 * @param userID 用户名
	 * @return 
	 */
	public String getUserDN(String userID){
		String distinguishedName = "";
        String returnedAtts[] = { "objectClass","distinguishedName"};
        try { 
            String searchFilter = "(&(objectClass=user)(cn="+userID+"))";
            SearchControls searchCtls = new SearchControls();
            searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            searchCtls.setReturningAttributes(returnedAtts);
            NamingEnumeration answer = ldapContext.search("", searchFilter,searchCtls);
            if (answer.hasMoreElements()) {
				SearchResult sr = (SearchResult) answer.next();
				Attributes attrs = sr.getAttributes();
				if (attrs != null) {
					Attribute attr = attrs.get("distinguishedName");
                    if(attr != null) {
                    	distinguishedName = (String)attr.get();
                    }
				}
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

		return distinguishedName;
	}
	
	/**
	 * 获取除域之外的用户DN
	 * @param userid
	 * @return
	 */
	public String getUserName(String userid){
		String returnValue = "";

        try { 
        	String userDN = getUserDN(userid);
	        
        	returnValue = userDN.replace(replaceSuffix, "");
        }catch (Exception e) {
            e.printStackTrace();
        }
        
		return returnValue;
	}
	
	/**
	 * 查找是否存在用户
	 * 
	 * @param userID 用户名
	 * @return false=不存在；true=存在
	 */
	public boolean userIsExist(String userID){
		boolean returnValue = false;
		
		String distinguishedName = getUserDN(userID);
		
		if(null != distinguishedName && !"".equals(distinguishedName)){
			returnValue = true;
        }
		
		return returnValue;
	}
	
	/**
	 * 添加用户
	 * 
	 * @param userName 用户姓名
	 * @param userID 用户帐号
	 * @param password 用户密码
	 * @param employeeID 员工ID
	 * @return true:成功；false:失败
	 */
	public boolean addUser(String userName, String userID, String password, String employeeID){
		boolean returnValue = false;
		
		try{
			String cnUserName = "cn="+userID;
			
	        Attributes attrs = new BasicAttributes(true);   

	        attrs.put("objectClass", "user");   
	        
	        attrs.put("cn", userID);   
	        attrs.put("sAMAccountName", userID);
	        
	        attrs.put("sn", userName);
	        attrs.put("name", userName);
	        attrs.put("displayName", userName);
	        attrs.put("employeeid", employeeID);
	        attrs.put("description", userID+"-"+userName+"-"+employeeID);
	        attrs.put("userPrincipalName", userID+"@"+domainSuffix);   
	        
	        attrs.put("userAccountControl", Integer.toString(UF_NORMAL_ACCOUNT   
	                + UF_PASSWD_NOTREQD + UF_PASSWORD_EXPIRED + UF_ACCOUNTDISABLE));   
	  
	        // Create the context   
	        Context result = ldapContext.createSubcontext(cnUserName,attrs);   
	  
	        ModificationItem[] mods = new ModificationItem[2];   
	  
	        // Replace the "unicdodePwd" attribute with a new value   
	        // Password must be both Unicode and a quoted string   
	        String newQuotedPassword = "\""+password+"\"";   
	        byte[] newUnicodePassword = newQuotedPassword.getBytes("UTF-16LE");   
	  
	        mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,   
	                new BasicAttribute("unicodePwd", newUnicodePassword));   
	        mods[1] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE,   
	                new BasicAttribute("userAccountControl", Integer   
	                        .toString(UF_NORMAL_ACCOUNT + UF_PASSWORD_EXPIRED)));   
	  
	        // Perform the update   
	        ldapContext.modifyAttributes(cnUserName, mods);   

	        returnValue = true;
	        
		}catch(Exception e){
			e.printStackTrace();
		}
		
		return returnValue;
	}
	
	/**
	 * 删除用户
	 * 
	 * @param userID
	 * @return
	 */
	public boolean removeUser(String userID){
		boolean returnValue = false;
		
		try{
			String userDN = getUserName(userID);
			ldapContext.destroySubcontext(userDN); 
			returnValue = true;
		}catch(Exception e){
			e.printStackTrace(System.err);
		}
		
		return returnValue;
	}
	
	public boolean modifyPassword(String userID,String oldPassword,String newPassword){
		boolean returnValue = false;
		
		try{
			ModificationItem[] mods = new ModificationItem[2]; 
	        String oldQuotedPassword = "\"" + oldPassword + "\"";
	        byte[] oldUnicodePassword = oldQuotedPassword.getBytes("UTF-16LE"); 
	        String newQuotedPassword = "\"" + newPassword + "\"";
	        byte[] newUnicodePassword = newQuotedPassword.getBytes("UTF-16LE"); 
	        //mods[0] = new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute("unicodePwd", newUnicodePassword));
	        mods[0] = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, new BasicAttribute("unicodePwd", oldUnicodePassword));            
	        mods[1] = new ModificationItem(DirContext.ADD_ATTRIBUTE, new BasicAttribute("unicodePwd", newUnicodePassword));            

	        //查找某一个用户信息
	        String returnedAtts[] = { "distinguishedName" };
	        
	        String searchFilter = "(&(objectClass=user)(cn="+userID+"))";
	        SearchControls searchCtls = new SearchControls();
	        searchCtls.setSearchScope(SearchControls.SUBTREE_SCOPE);
	        searchCtls.setReturningAttributes(returnedAtts);
	        NamingEnumeration answer = ldapContext.search("", searchFilter,searchCtls);
	        if (answer.hasMoreElements()) {
				SearchResult sr = (SearchResult) answer.next();
				Attributes attrs = sr.getAttributes();
				if (attrs != null) {
					NamingEnumeration ae = attrs.getAll();
					Attribute attr = (Attribute) ae.next();
					NamingEnumeration e = attr.getAll();
					String userName = (String) e.next();
					if(userName != null && userName.indexOf("DC=") > 0) {
						userName = userName.substring(0, userName.indexOf("DC=")-1);
					}
					//修改密码
					ldapContext.modifyAttributes(userName,mods);
				}
	        }
	        
	        returnValue = true;
		}catch(Exception e){
			e.printStackTrace(System.err);
		}
		
		return returnValue;
	}
	/**
	 * config file
	 * 
	 * @param propertiesName
	 * @return
	 */
	public static Properties getProperties(String propertiesName){
		Properties prop = new Properties();
    	
    	String currpath = new File("").getAbsolutePath()+"\\"+propertiesName;
		try {
			InputStream in = new FileInputStream(new File(currpath));
			prop.load(in);
		} catch (FileNotFoundException e) {
			e.printStackTrace(System.err);
		} catch (IOException e) {
			e.printStackTrace(System.err);
		}
    	
    	return prop;
	}
	
	public ActiveDirectoryUtil(){

	}
	
	/**
	 * init
	 * 
	 * @return true:success;false:fail
	 */
	public boolean init(){
		try {
			Properties properties = getProperties("LdapConfig.properties");
			
			// 加载AD证书
	        String keystore = new File("").getAbsolutePath()+"\\"+properties.getProperty("keystore");
	        String keyPassword = "changeit";
	        System.setProperty("javax.net.ssl.trustStore", keystore);   
	        System.setProperty("javax.net.ssl.trustStorePassword", keyPassword);
	        
			// 参数配置
	        adminName = properties.getProperty("adminName");
	        adminPassword = properties.getProperty("adminPwd");
	        
			DC1 = properties.getProperty("DC1");
	        DC2 = properties.getProperty("DC2");
	        
	        IP = properties.getProperty("IP");
	        PORT = properties.getProperty("PORT");
	        URL = "ldap://"+IP+":"+PORT +"/DC="+DC1+",DC="+DC2;
	        
	        replaceSuffix = ",DC="+DC1+",DC="+DC2;
	        domainSuffix = properties.getProperty("domainSuffix");
	        mailSuffix = properties.getProperty("mailSuffix");
	        
	        Properties env = new Properties();
	        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");// java.naming.factory.initial   
	        env.put(Context.SECURITY_AUTHENTICATION, "simple");// java.naming.security.authentication   
	        env.put(Context.SECURITY_PRINCIPAL,adminName);// java.naming.security.principal   
	        env.put(Context.SECURITY_CREDENTIALS, adminPassword);// java.naming.security.credentials   
	        env.put(Context.SECURITY_PROTOCOL, "ssl");   
	        env.put(Context.PROVIDER_URL, URL);// java.naming.provider.url   
	        
			ldapContext = new InitialLdapContext(env, null);
			
		} catch (NamingException e) {
			e.printStackTrace(System.err);
		}
		
		return (null != ldapContext?true:false);
	}

	/**
	 * remember to release it
	 */
	public void release(){
		try {
			if(null != ldapContext){
				ldapContext.close();
			}
		} catch (NamingException e) {
			e.printStackTrace(System.err);
		}
	}
}
