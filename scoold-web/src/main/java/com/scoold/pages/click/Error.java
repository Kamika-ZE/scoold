/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.scoold.pages.click;

import com.scoold.core.Language;
import com.scoold.db.AbstractDAOFactory;
import com.scoold.db.AbstractDAOUtils;
import com.scoold.pages.BasePage;
import java.util.Map;
import java.util.Random;
import org.apache.click.util.ErrorPage;

/**
 *
 * @author alexb
 */
public class Error extends ErrorPage {

    public String title;
	public Map<String, String> lang;
	public AbstractDAOUtils daoutils;

    public Error() {
		daoutils = AbstractDAOFactory.getDefaultDAOFactory().getDAOUtils();
		lang = Language.readLanguage(getContext().getLocale());
        title = lang.get("error.title");
		addModel("APPNAME", BasePage.APPNAME);
		addModel("currentLocale", getContext().getLocale());
		addModel("DESCRIPTION", BasePage.DESCRIPTION);
		addModel("KEYWORDS", BasePage.KEYWORDS);
		addModel("rand", (new Random().nextInt(1337) + 100));
    }

	public String getTemplate() {
		return "click/error.htm";
	}


} 
