/*
 * Copyright 2016 JBoss, by Red Hat, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.dashbuilder.client.navigation.widget;

import java.util.List;
import java.util.stream.Collectors;
import javax.enterprise.context.Dependent;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import com.google.gwt.user.client.ui.IsWidget;
import org.dashbuilder.client.navigation.NavigationManager;
import org.dashbuilder.client.navigation.plugin.PerspectivePluginManager;
import org.dashbuilder.navigation.NavItem;
import org.uberfire.ext.plugin.event.PluginSaved;
import org.uberfire.ext.plugin.model.Plugin;

@Dependent
public class NavTabListWidget extends BaseNavWidget implements HasDefaultNavItem {

    public interface View extends NavWidgetView<NavTabListWidget> {

        void showContent(IsWidget widget);

        void deadlockError();
    }

    View view;
    PerspectivePluginManager perspectivePluginManager;
    String defaultNavItemId = null;

    @Inject
    public NavTabListWidget(View view, NavigationManager navigationManager, PerspectivePluginManager perspectivePluginManager) {
        super(view, navigationManager);
        this.view = view;
        this.perspectivePluginManager = perspectivePluginManager;
        super.setMaxLevels(1);
    }

    @Override
    public boolean areSubGroupsSupported() {
        return false;
    }

    @Override
    public String getDefaultNavItemId() {
        return defaultNavItemId;
    }

    @Override
    public void setDefaultNavItemId(String defaultNavItemId) {
        this.defaultNavItemId = defaultNavItemId;
    }

    @Override
    public void show(List<NavItem> itemList) {
        // Discard everything but runtime perspectives
        List<NavItem> itemsFiltered = itemList.stream()
                .filter(perspectivePluginManager::isRuntimePerspective)
                .collect(Collectors.toList());

        super.show(itemsFiltered);

        // Force the display or either the default item configured or the first one available
        if (defaultNavItemId != null || !navItemList.isEmpty()) {
            setSelectedItem(defaultNavItemId != null ? defaultNavItemId : navItemList.get(0).getId());
        }
    }

    public void showPerspective(NavItem navItem) {
        // Only runtime perspectives can be displayed under the selected tab
        String perspectiveId = perspectivePluginManager.getRuntimePerspectiveId(navItem);
        if (perspectiveId != null) {
            showPerspective(perspectiveId);
        }
    }

    public void showPerspective(String perspectiveId) {
        perspectivePluginManager.buildPerspectiveWidget(perspectiveId, view::showContent, view::deadlockError);
    }

    // When an tab is selected its perspective is shown right under the tab

    @Override
    public boolean setSelectedItem(String id) {
        boolean selected = super.setSelectedItem(id);
        if (selected) {
            showPerspective(getItemSelected());
        }
        return selected;
    }

    @Override
    public void onItemClicked(NavItem navItem) {
        super.onItemClicked(navItem);
        showPerspective(navItem);
    }

    // Catch changes on runtime perspectives so as to display the most up to date changes

    private void onPluginSaved(@Observes PluginSaved event) {
        Plugin plugin = event.getPlugin();
        String pluginName = plugin.getName();
        String selectedPerspectiveId = perspectivePluginManager.getRuntimePerspectiveId(itemSelected);
        if (selectedPerspectiveId != null && selectedPerspectiveId.equals(pluginName)) {
            showPerspective(itemSelected);
        }
    }
}