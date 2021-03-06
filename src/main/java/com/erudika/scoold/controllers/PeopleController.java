/*
 * Copyright 2013-2020 Erudika. https://erudika.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * For issues and patches go to: https://github.com/erudika
 */
package com.erudika.scoold.controllers;

import com.erudika.para.client.ParaClient;
import com.erudika.para.core.ParaObject;
import com.erudika.para.utils.Config;
import com.erudika.para.utils.Pager;
import com.erudika.para.utils.Utils;
import static com.erudika.scoold.ScooldServer.PEOPLELINK;
import static com.erudika.scoold.ScooldServer.SIGNINLINK;
import com.erudika.scoold.core.Profile;
import com.erudika.scoold.utils.HttpUtils;
import com.erudika.scoold.utils.ScooldUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Entity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 *
 * @author Alex Bogdanovski [alex@erudika.com]
 */
@Controller
@RequestMapping("/people")
public class PeopleController {

	private final ScooldUtils utils;
	private final ParaClient pc;

	@Inject
	public PeopleController(ScooldUtils utils) {
		this.utils = utils;
		this.pc = utils.getParaClient();
	}

	@GetMapping
	public String get(@RequestParam(required = false, defaultValue = Config._TIMESTAMP) String sortby,
			@RequestParam(required = false, defaultValue = "*") String q, HttpServletRequest req, Model model) {
		if (!utils.isDefaultSpacePublic() && !utils.isAuthenticated(req)) {
			return "redirect:" + SIGNINLINK + "?returnto=" + PEOPLELINK;
		}
		Profile authUser = utils.getAuthUser(req);
		Pager itemcount = utils.getPager("page", req);
		itemcount.setSortby(sortby);
		// [space query filter] + original query string
		String qs = utils.sanitizeQueryString(q, req);
		if (req.getParameter("bulkedit") != null && utils.isAdmin(authUser)) {
			qs = q;
		} else {
			qs = qs.replaceAll("properties\\.space:", "properties.spaces:");
		}

		List<Profile> userlist = pc.findQuery(Utils.type(Profile.class), qs, itemcount);
		model.addAttribute("path", "people.vm");
		model.addAttribute("title", utils.getLang(req).get("people.title"));
		model.addAttribute("peopleSelected", "navbtn-hover");
		model.addAttribute("itemcount", itemcount);
		model.addAttribute("userlist", userlist);
		if (req.getParameter("bulkedit") != null && utils.isAdmin(authUser)) {
			List<ParaObject> spaces = pc.findQuery("scooldspace", "*", new Pager(Config.DEFAULT_LIMIT));
			model.addAttribute("spaces", spaces);
		}
		return "base";
	}

	@PostMapping("/bulk-edit")
	public String bulkEdit(@RequestParam(required = false) String[] selectedUsers,
			@RequestParam(required = false) final String[] selectedSpaces, HttpServletRequest req) {
		Profile authUser = utils.getAuthUser(req);
		boolean isAdmin = utils.isAdmin(authUser);
		String operation = req.getParameter("operation");
		String selection = req.getParameter("selection");
		if (isAdmin && ("all".equals(selection) || selectedUsers != null)) {
			// find all user objects even if there are more than 10000 users in the system
			Pager pager = new Pager(1, "_docid", false, Config.MAX_ITEMS_PER_PAGE);
			List<Profile> profiles;
			ArrayList<Map<String, Object>> toUpdate = new ArrayList<>();
			List<String> spaces = (selectedSpaces == null || selectedSpaces.length == 0) ?
					Collections.emptyList() : Arrays.asList(selectedSpaces);
			do {
				String query = (selection == null || "selected".equals(selection)) ?
						Config._ID + ":(\"" + String.join("\" \"", selectedUsers) + "\")" : "*";
				profiles = pc.findQuery(Utils.type(Profile.class), query, pager).stream().
						filter(p -> !utils.isMod(((Profile) p))).
						map(p -> {
							if ("add".equals(operation)) {
								((Profile) p).getSpaces().addAll(spaces);
							} else if ("remove".equals(operation)) {
								((Profile) p).getSpaces().removeAll(spaces);
							} else {
								((Profile) p).setSpaces(new HashSet<String>(spaces));
							}
							Map<String, Object> profile = new HashMap<>();
							profile.put(Config._ID, p.getId());
							profile.put("spaces", ((Profile) p).getSpaces());
							toUpdate.add(profile);
							return (Profile) p;
						}).collect(Collectors.toList());
				if (!toUpdate.isEmpty()) {
					// partial batch update
					pc.invokePatch("_batch", Entity.json(toUpdate));
					toUpdate.clear();
				}
			} while (!profiles.isEmpty());
		}
		return "redirect:" + PEOPLELINK + (isAdmin ? "?" + req.getQueryString() : "");
	}

	@GetMapping("/avatar")
	public void avatar(@RequestParam(required = false) String url, HttpServletResponse res, Model model) {
		HttpUtils.getAvatar(url, res);
	}
}
