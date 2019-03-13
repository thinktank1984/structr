/*
 * Copyright (C) 2010-2019 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
$(document).ready(function() {
	Structr.registerModule(_Dashboard);
});

var _Dashboard = {
	_moduleName: 'dashboard',
	dashboard: undefined,
	aboutMe: undefined,
	meObj: undefined,

	init: function() {
		$('#dashboard').remove();
	},
	unload: function() {},
	onload: function() {
		_Dashboard.init();
		Structr.updateMainHelpLink('https://support.structr.com/article/202');

		main.append('<div id="dashboard"></div>');
		_Dashboard.dashboard = $('#dashboard', main);

		_Dashboard.appendDatabaseSelectionBox();

		_Dashboard.aboutMe = _Dashboard.appendBox('About Me', 'about-me');
		_Dashboard.aboutMe.append('<div class="dashboard-info">You are currently logged in as <b>' + me.username + '<b>.</div>');
		_Dashboard.aboutMe.append('<div class="dashboard-info admin red"></div>');
		_Dashboard.aboutMe.append('<table class="props"></table>');

		$.get(rootUrl + '/me/ui', function(data) {
			_Dashboard.meObj = data.result;
			var t = $('table', _Dashboard.aboutMe);
			t.append('<tr><td class="key">ID</td><td>' + _Dashboard.meObj.id + '</td></tr>');
			t.append('<tr><td class="key">E-Mail</td><td>' + (_Dashboard.meObj.eMail || '') + '</td></tr>');
			t.append('<tr><td class="key">Working Directory</td><td>' + (_Dashboard.meObj.workingDirectory ? _Dashboard.meObj.workingDirectory.name : '') + '</td></tr>');
			t.append('<tr><td class="key">Session ID(s)</td><td>' + _Dashboard.meObj.sessionIds.join('<br>') + '</td></tr>');
			t.append('<tr><td class="key">Groups</td><td>' + _Dashboard.meObj.groups.map(function(g) { return g.name; }).join(', ') + '</td></tr>');

			if (_Dashboard.meObj.isAdmin) {
				_Dashboard.appendDeploymentBox();
				_Dashboard.appendMaintenanceBox();
			}
		});
		_Dashboard.checkAdmin();

		_Dashboard.aboutMe.append('<button class="action" id="clear-local-storage-on-server">Reset stored UI settings</button>');
		$('#clear-local-storage-on-server').on('click', function() {
			_Dashboard.clearLocalStorageOnServer();
		});

		_Dashboard.appendAboutBox();

		$('.dashboard-info a.internal-link').on('click', function() {
			$('#' + $(this).text() + '_').click();
		});

		$(window).off('resize');
		$(window).on('resize', function() {
			Structr.resize();
		});

		Structr.unblockMenu(100);

	},
	appendBox: function(heading, id) {
		_Dashboard.dashboard.append('<div id="' + id + '" class="dashboard-box"><div class="dashboard-header"><h2>' + heading + '</h2></div></div>');
		return $('#' + id, main);
	},
	checkAdmin: function() {
		if (me.isAdmin && _Dashboard.aboutMe && _Dashboard.aboutMe.length && _Dashboard.aboutMe.find('admin').length === 0) {
			$('.dashboard-info.admin', _Dashboard.aboutMe).text('You have admin rights.');
		}
	},
	displayVersion: function(obj) {
		return (obj.version ? ' (v' + obj.version + ')': '');
	},
	displayName: function(obj) {
		return fitStringToWidth(obj.name, 160);
	},
	clearLocalStorageOnServer: function() {

		var clear = function (userId) {
			Command.setProperty(userId, 'localStorage', null, false, function() {
				blinkGreen($('#clear-local-storage-on-server'));
				LSWrapper.clear();
			});
		};

		if (!_Dashboard.meObj) {
			Command.rest("/me/ui", function (result) {
				clear(result[0].id);
			});
		} else {
			clear(_Dashboard.meObj.id);
		}
	},
	appendDatabaseSelectionBox: function() {

		_Dashboard.appendBox('Database Connections', 'database-connections');

		Structr.fetchHtmlTemplate('dashboard/database.connections', { }, function (html) {

			var parent = $('#database-connections');

			parent.append(html);

			_Dashboard.loadDatabaseSelectionBox();
		});
	},
	loadDatabaseSelectionBox: function() {

		$.post(
			rootUrl + '/maintenance/manageDatabases',
			JSON.stringify({ command: "list" }),
			function(data) {

				var body = $('#database-connection-table-body');
				body.empty();

				data.result.forEach(function(result) {

					Structr.fetchHtmlTemplate('dashboard/connection.row', _Dashboard.mapConnectionResult(result), function (html) {

						body.append(html);

						$('button#button_' + result.name).on('click', function(btn) {

							Structr.showLoadingMessage(
								'Changing database connection to ' + result.name,
								'Please wait until the change has been applied. If you don\'t have a valid session ID in the other database, you will need to re-login after the change.',
								200
							);

							$.post(
								rootUrl + '/maintenance/manageDatabases',
								JSON.stringify({
									command: 'activate',
									name: result.name
								}),
								function() {
									Structr.hideLoadingMessage();
									_Dashboard.onload();
								}
							);
						});
					});
				});
			}
		);
	},
	appendAboutBox: function () {

		var aboutStructrBox = _Dashboard.appendBox('About Structr', 'about-structr');
		var aboutStructrTable = $('<table class="props"></table>').appendTo(aboutStructrBox);

		$.get(rootUrl + '/_env', function(data) {
			var envInfo = data.result;

			if (envInfo.edition) {
				var tooltipText = 'Structr ' + envInfo.edition + ' Edition';
				var versionInfo = '<i title="' + tooltipText + '" class="' + _Icons.getFullSpriteClass(_Icons.getIconForEdition(envInfo.edition)) + '"></i> (' + tooltipText + ')';

				aboutStructrTable.append('<tr><td class="key">Edition</td><td>' + versionInfo + '</td></tr>');
				aboutStructrTable.append('<tr><td class="key">Licensee</td><td>' + (envInfo.licensee || 'Unlicensed') + '</td></tr>');
				aboutStructrTable.append('<tr><td class="key">Host ID</td><td>' + (envInfo.hostId || '') + '</td></tr>');
				aboutStructrTable.append('<tr><td class="key">License Start Date</td><td>' + (envInfo.startDate || '-') + '</td></tr>');
				aboutStructrTable.append('<tr><td class="key">License End Date</td><td class="end-date">' + (envInfo.endDate || '-') + '</td></tr>');

				_Dashboard.checkLicenseEnd(envInfo, $('.end-date', aboutStructrTable));
			}
		});

	},
	checkLicenseEnd:function (envInfo, element, cfg) {

		if (envInfo && envInfo.endDate && element) {

			var showMessage = true;
			var daysLeft = Math.ceil((new Date(envInfo.endDate) - new Date()) / 86400000) + 1;

			var config = {
				element: element,
				appendToElement: element
			};
			config = $.extend(config, cfg);

			if (daysLeft <= 0) {

				config.customToggleIcon = _Icons.exclamation_icon;
				config.text = "Your Structr <b>license has expired</b>. Upon restart the Community edition will be loaded.";

			} else if (daysLeft <= 7) {

				config.customToggleIcon = _Icons.error_icon;
				config.text = "Your Structr <b>license will expire in less than a week</b>. After that the Community edition will be loaded.";

			} else {
				showMessage = false;
			}

			if (showMessage) {

				config.text += " Please get in touch via <b>licensing@structr.com</b> to renew your license.";
				Structr.appendInfoTextToElement(config);
			}

		}
	},
	appendMaintenanceBox: function () {

		var maintenanceBox  = _Dashboard.appendBox('Global schema methods', 'maintenance');
		var maintenanceList = $('<div></div>').appendTo(maintenanceBox);

		$.get(rootUrl + '/SchemaMethod?schemaNode=&sort=name', function(data) {

			data.result.forEach(function(result) {

				var methodRow = $('<div class="dashboard-info" style="border-bottom: 1px solid #ddd; padding-bottom: 2px;"></div>');
				var methodName = $('<span style="line-height: 2em;">' + result.name + ' </span>');

				methodRow.append(methodName).append('<button id="run-' + result.id + '" class="pull-right" style="margin-left: 1em;">Run now</button>');
				maintenanceList.append(methodRow);

				var cleanedComment = (result.comment && result.comment.trim() !== '') ? result.comment.replaceAll("\n", "<br>") : '';

				if (cleanedComment.trim() !== '') {
					Structr.appendInfoTextToElement({
						element: methodName,
						text: cleanedComment,
						helpElementCss: {
							"line-height": "initial"
						}
					});
				}

				$('button#run-' + result.id).on('click', function() {

					Structr.dialog('Run global schema method ' + result.name, function() {}, function() {
						$('#run-method').remove();
						$('#clear-log').remove();
					});

					dialogBtn.prepend('<button id="run-method">Run</button>');
					dialogBtn.append('<button id="clear-log">Clear output</button>');

					var paramsOuterBox = $('<div id="params"><h3 class="heading-narrow">Parameters</h3></div>');
					var paramsBox = $('<div></div>');
					paramsOuterBox.append(paramsBox);
					var addParamBtn = $('<i title="Add parameter" class="button ' + _Icons.getFullSpriteClass(_Icons.add_icon) + '" />');
					paramsBox.append(addParamBtn);
					dialog.append(paramsOuterBox);

					if (cleanedComment.trim() !== '') {
						dialog.append('<div id="global-method-comment"><h3 class="heading-narrow">Comment</h3>' + cleanedComment + '</div>');
					}

					Structr.appendInfoTextToElement({
						element: $('#params h3'),
						text: "Parameters can be accessed by using the <code>retrieve()</code> function.",
						css: { marginLeft: "5px" },
						helpElementCss: { fontSize: "12px" }
					});

					addParamBtn.on('click', function() {
						var newParam = $('<div class="param"><input class="param-name" placeholder="Parameter name"> : <input class="param-value" placeholder="Parameter value"></div>');
						var removeParam = $('<i class="button ' + _Icons.getFullSpriteClass(_Icons.delete_icon) + '" alt="Remove parameter" title="Remove parameter"/>');

						newParam.append(removeParam);
						removeParam.on('click', function() {
							newParam.remove();
						});
						paramsBox.append(newParam);
					});

					dialog.append('<h3>Method output</h3>');
					dialog.append('<pre id="log-output"></pre>');

					$('#run-method').on('click', function() {

						$('#log-output').empty();
						$('#log-output').append('Running method..\n');

						var params = {};
						$('#params .param').each(function (index, el) {
							var name = $('.param-name', el).val();
							var val = $('.param-value', el).val();
							if (name) {
								params[name] = val;
							}
						});

						$.ajax({
							url: rootUrl + '/maintenance/globalSchemaMethods/' + result.name,
							data: JSON.stringify(params),
							method: 'POST',
							complete: function(data) {
								$('#log-output').append(data.responseText);
								$('#log-output').append('Done.');
							}
						});
					});

					$('#clear-log').on('click', function() {
						$('#log-output').empty();
					});
				});
			});
		});

	},
	appendDeploymentBox: function () {

		var deploymentBox  = _Dashboard.appendBox('Deployment', 'deployment');
		var container      = $('<div></div>').appendTo(deploymentBox);

		container.append('<h3>Import application from local directory</h3>');
		container.append('<div><input type="text" id="deployment-source-input" size="60" placeholder="Enter directory path..."> <button class="action" id="do-import">Import from local directory</button></div>');

		container.append('<h3>Import application from URL</h3>');
		container.append('<form action="/structr/deploy" method="POST" enctype="multipart/form-data"><input type="hidden" name="redirectUrl" value="' + window.location.href + '"><input type="text" id="deployment-url-input" size="60" placeholder="Enter download URL..." name="downloadUrl"> <button type="submit" class="action">Import from URL</button></form>');

		container.append('<h3>Export application to local directory</h3>');
		container.append('<div><input type="text" id="deployment-target-input" size="60" placeholder="Enter directory path..."> <button class="action" id="do-export">Export to local directory</button></div>');

		$('button#do-import').on('click', function() {
			var location = $('#deployment-source-input').val();
			if (location && location.length) {
				_Dashboard.deploy('import', location);
			} else {
				// show error message
			}
		});

		$('button#do-export').on('click', function() {
			var location = $('#deployment-target-input').val();
			if (location && location.length) {
				_Dashboard.deploy('export', location);
			} else {
				// show error message
			}
		});
	},
	deploy: function(mode, location) {

		var data = {
			mode: mode
		};

		if (mode === 'import') {
			data['source'] = location;
		} else if (mode === 'export') {
			data['target'] = location;
		}

		$.ajax({
			url: rootUrl + '/maintenance/deploy',
			data: JSON.stringify(data),
			method: 'POST'
		});

	},
	mapConnectionResult: function(result) {

		var activeString = result.active ? '<b>active</b>' : '-';
		var button       = result.active ? '' : '<button class="action" id="button_' + result.name + '">Connect</button>';

		if (result.driver === 'org.structr.memory.MemoryDatabaseService') {

			return {

				name:     result.name,
				type:     'in-memory',
				url:      '-',
				username: '-',
				active:   activeString,
				button:   button

			};

		} else {

			return {

				name:     result.name,
				type:     'neo4j',
				url:      result.url,
				username: result.username,
				active:   activeString,
				button:   button
			};
		}
	}
};

/*
		<td style="text-align: left;">${config.name}</td>
		<td style="text-align: left;">${config.type}</td>
		<td style="text-align: left;">${config.url}</td>
		<td style="text-align: left;">${config.username}</td>
		<td style="text-align: right;">${config.active}</td>
		settings.put("name",                  name);
		settings.put("driver",                Settings.DatabaseDriver.getPrefixedValue(prefix));
		settings.put("mode",                  Settings.DatabaseDriverMode.getPrefixedValue(prefix));
		settings.put("url",                   Settings.ConnectionUrl.getPrefixedValue(prefix));
		settings.put("username",              Settings.ConnectionUser.getPrefixedValue(prefix));
		settings.put("password",              Settings.ConnectionPassword.getPrefixedValue(prefix));
		settings.put("tenantIdentifier",      Settings.TenantIdentifier.getPrefixedValue(prefix));
		settings.put("relationshipCacheSize", Settings.RelationshipCacheSize.getPrefixedValue(prefix));
		settings.put("nodeCacheSize",         Settings.NodeCacheSize.getPrefixedValue(prefix));
		settings.put("uuidCachesize",         Settings.UuidCacheSize.getPrefixedValue(prefix));
		settings.put("forceStreaming",        Settings.ForceResultStreaming.getPrefixedValue(prefix));
 */