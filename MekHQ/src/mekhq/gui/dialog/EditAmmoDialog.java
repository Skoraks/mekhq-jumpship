/*
 * Copyright (C) 2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MekHQ.
 *
 * MekHQ is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MekHQ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MekHQ was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package mekhq.gui.dialog;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;

import megamek.client.ui.util.UIUtil;
import megamek.common.equipment.AmmoMounted;
import megamek.common.units.Entity;
import megamek.logging.MMLogger;

/**
 * Dialog for editing ammo consumption of units during manual scenario resolution.
 * Allows users to adjust the remaining shots for each ammo bin on a unit.
 */
public class EditAmmoDialog extends JDialog {
    private static final MMLogger logger = MMLogger.create(EditAmmoDialog.class);

    private final Entity entity;
    private final List<JSpinner> ammoSpinners = new ArrayList<>();
    private final List<AmmoMounted> ammoMounteds = new ArrayList<>();
    private final List<Integer> correctedOriginalShots = new ArrayList<>();
    private boolean confirmed = false;

    public EditAmmoDialog(Frame parent, Entity entity) {
        super(parent, "Edit Ammo Consumption", true);
        this.entity = entity;
        
        initComponents();
        setLocationRelativeTo(parent);
        pack();
    }

    private void initComponents() {
        setLayout(new BorderLayout());
        
        // Main panel with ammo controls
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBorder(new TitledBorder("Ammo Bins - " + entity.getDisplayName()));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        // Header row
        gbc.gridx = 0; gbc.gridy = 0;
        mainPanel.add(new JLabel("Weapon/Location:"), gbc);
        gbc.gridx = 1;
        mainPanel.add(new JLabel("Ammo Type:"), gbc);
        gbc.gridx = 2;
        mainPanel.add(new JLabel("Shots Remaining:"), gbc);
        
        // Add ammo bins
        int row = 1;
        for (AmmoMounted ammo : entity.getAmmo()) {
            if (ammo != null && ammo.getType() != null) {
                // Weapon name and location
                gbc.gridx = 0; gbc.gridy = row;
                String weaponName;
                String location = entity.getLocationName(ammo.getLocation());
                
                // Try to find the linked weapon
                if (ammo.getLinked() != null) {
                    weaponName = ammo.getLinked().getName();
                } else if (ammo.getLinkedBy() != null) {
                    weaponName = ammo.getLinkedBy().getName();
                } else {
                    // Use ammo type name as fallback
                    weaponName = ammo.getType().getName();
                }
                
                String displayText = weaponName + " (" + location + ")";
                mainPanel.add(new JLabel(displayText), gbc);
                
                // Ammo type
                gbc.gridx = 1;
                String ammoName = ammo.getType().getName();
                if (ammo.isHotLoaded()) {
                    ammoName += " (Hot-Loaded)";
                }
                mainPanel.add(new JLabel(ammoName), gbc);
                
                // Shots remaining spinner
                gbc.gridx = 2;
                try {
                    int currentShots = ammo.getBaseShotsLeft();
                    int originalShots = ammo.getOriginalShots();
                    
                    // Debug logging to understand the values
                    logger.info("Ammo {}: type={}, current={}, original={}, location={}", 
                        weaponName, ammo.getType().getName(), currentShots, originalShots, 
                        entity.getLocationName(ammo.getLocation()));
                    
                    // Handle case where original shots is 0 (possible data issue)
                    if (originalShots == 0) {
                        // Try to get capacity from ammo type as fallback
                        int ammoCapacity = ammo.getType().getShots();
                        if (ammoCapacity > 0) {
                            logger.warn("Original shots is 0, using ammo type capacity ({}) as fallback", 
                                ammoCapacity);
                            originalShots = ammoCapacity;
                        } else {
                            // Last resort: use a reasonable default based on ammo type
                            originalShots = Math.max(currentShots, 1);
                            logger.warn("Both original and capacity are 0, using default ({})", 
                                originalShots);
                        }
                    }
                    
                    // Ensure we have valid values and handle inconsistent state
                    currentShots = Math.max(0, currentShots);
                    // If current shots exceed original, cap at original
                    if (currentShots > originalShots) {
                        logger.warn("Ammo {} has more shots ({}) than original ({}), capping at original", 
                            ammo.getType().getName(), currentShots, originalShots);
                        currentShots = originalShots;
                    }
                    // Original shots should always be the maximum, regardless of current shots
                    logger.debug("Creating spinner for {}: current={}, original={}, max={}", 
                        ammo.getType().getName(), currentShots, originalShots, originalShots);
                    SpinnerNumberModel spinnerModel = new SpinnerNumberModel(
                        currentShots, 0, originalShots, 1);
                    JSpinner spinner = new JSpinner(spinnerModel);
                    spinner.setToolTipText("Adjust remaining shots (0-" + originalShots + ")");
                    
                    // Add spinner and update lists only if everything succeeds
                    ammoMounteds.add(ammo);
                    ammoSpinners.add(spinner);
                    // Store corrected original shots for validation
                    correctedOriginalShots.add(originalShots);
                    mainPanel.add(spinner, gbc);
                } catch (Exception e) {
                    logger.error("Error creating spinner for ammo: " + ammo.getType().getName(), e);
                    // Create error display in place of spinner
                    mainPanel.add(new JLabel("Error: " + e.getMessage()), gbc);
                }
                
                row++;
            }
        }
        
        // Check if we have any ammo at all (not just successful spinners)
        boolean hasAnyAmmo = false;
        for (AmmoMounted ammo : entity.getAmmo()) {
            if (ammo != null && ammo.getType() != null) {
                hasAnyAmmo = true;
                break;
            }
        }
        
        // If no ammo found
        if (!hasAnyAmmo) {
            gbc.gridx = 0; gbc.gridy = 1;
            gbc.gridwidth = 3;
            gbc.anchor = GridBagConstraints.CENTER;
            mainPanel.add(new JLabel("No ammo bins found on this unit"), gbc);
        }
        
        add(mainPanel, BorderLayout.CENTER);
        
        // Button panel
        JPanel buttonPanel = new JPanel(new FlowLayout());
        
        JButton btnZero = new JButton("Set All to Zero");
        btnZero.addActionListener(e -> setAllToZero());
        buttonPanel.add(btnZero);
        
        JButton btnFull = new JButton("Set All to Full");
        btnFull.addActionListener(e -> setAllToFull());
        buttonPanel.add(btnFull);
        
        JButton btnCancel = new JButton("Cancel");
        btnCancel.addActionListener(e -> dispose());
        buttonPanel.add(btnCancel);
        
        JButton btnOK = new JButton("OK");
        btnOK.addActionListener(e -> confirmChanges());
        getRootPane().setDefaultButton(btnOK);
        buttonPanel.add(btnOK);
        
        add(buttonPanel, BorderLayout.SOUTH);
        
        // Set preferred size
        setPreferredSize(UIUtil.scaleForGUI(600, Math.max(200, row * 30 + 100)));
    }

    private void setAllToZero() {
        for (JSpinner spinner : ammoSpinners) {
            spinner.setValue(0);
        }
    }

    private void setAllToFull() {
        for (int i = 0; i < ammoSpinners.size(); i++) {
            if (i < correctedOriginalShots.size()) {
                ammoSpinners.get(i).setValue(correctedOriginalShots.get(i));
            }
        }
    }

    private void confirmChanges() {
        try {
            // Apply the changes to the entity
            for (int i = 0; i < ammoSpinners.size(); i++) {
                if (i >= ammoMounteds.size()) {
                    break; // Safety check
                }
                
                Integer spinnerValue = (Integer) ammoSpinners.get(i).getValue();
                if (spinnerValue == null) {
                    continue; // Skip null values
                }
                
                int newShots = spinnerValue;
                AmmoMounted ammo = ammoMounteds.get(i);
                
                if (ammo == null) {
                    continue; // Skip null ammo
                }
                
                // Validate the new value using corrected original shots
                int correctedOriginal = correctedOriginalShots.get(i);
                logger.debug("Validating ammo {}: newShots={}, correctedOriginal={}", 
                    ammo.getType().getName(), newShots, correctedOriginal);
                
                if (newShots < 0 || newShots > correctedOriginal) {
                    logger.warn("Invalid shot count for {}: newShots={}, correctedOriginal={}", 
                        ammo.getType().getName(), newShots, correctedOriginal);
                    JOptionPane.showMessageDialog(this,
                        "Invalid shot count for " + ammo.getType().getName() + 
                        ". Must be between 0 and " + correctedOriginal,
                        "Invalid Input", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                
                ammo.setShotsLeft(newShots);
            }
            
            confirmed = true;
        } catch (Exception e) {
            logger.error("Error confirming ammo changes", e);
            JOptionPane.showMessageDialog(this,
                "An error occurred while saving ammo changes: " + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
        }
        
        dispose();
    }

    public boolean wasConfirmed() {
        return confirmed;
    }
}
