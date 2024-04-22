package org.infinispan.cli.wizard.driver.swing;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.geom.AffineTransform;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import org.infinispan.cli.wizard.Checkbox;
import org.infinispan.cli.wizard.Choice;
import org.infinispan.cli.wizard.Directory;
import org.infinispan.cli.wizard.Driver;
import org.infinispan.cli.wizard.File;
import org.infinispan.cli.wizard.Input;
import org.infinispan.cli.wizard.Number;
import org.infinispan.cli.wizard.Page;
import org.infinispan.cli.wizard.Secret;
import org.infinispan.cli.wizard.Table;
import org.infinispan.cli.wizard.Text;
import org.infinispan.cli.wizard.Values;
import org.infinispan.cli.wizard.Wizard;
import org.infinispan.cli.wizard.YesNo;

public class SwingDriver implements Driver {
   private final Wizard wizard;
   private final Values values;
   private int current;
   private JComponent main;
   List<Supplier<Object>> suppliers = new ArrayList<>();
   Deque<Integer> history = new ArrayDeque<>();

   public SwingDriver(Wizard wizard, Values values) {
      this.wizard = wizard;
      this.values = values;
      this.current = 0;
   }

   @Override
   public Optional<Values> run() {
      CompletableFuture<Values> cf = new CompletableFuture<>();
      JFrame f = new JFrame(wizard.title());
      f.setSize(800, 600);
      f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
      BorderLayout layout = new BorderLayout();
      f.setLayout(layout);

      // The buttons at the bottom
      JPanel buttons = new JPanel();
      buttons.setLayout(new BorderLayout());
      buttons.add(new JSeparator(), BorderLayout.NORTH);

      Box buttonBox = Box.createHorizontalBox();
      buttonBox.setBorder(new EmptyBorder(new Insets(5, 10, 5, 10)));
      buttons.add(buttonBox, BorderLayout.EAST);
      f.add(buttons, BorderLayout.SOUTH);
      buttonBox.add(Box.createGlue());
      JButton close = new JButton("Close");
      buttonBox.add(close);
      buttonBox.add(Box.createHorizontalStrut(30));
      JButton back = new JButton("< Back");
      buttonBox.add(back);
      back.setEnabled(false);
      buttonBox.add(Box.createHorizontalStrut(10));
      JButton next = new JButton("Next >");
      buttonBox.add(next);
      back.addActionListener(e -> {
         formToValues();
         current = history.pop();
         next.setEnabled(true);
         back.setEnabled(current > 0);
         populate();
      });
      next.addActionListener(e -> {
         formToValues();
         Page.Action action = wizard.pages()[current].action().apply(values);
         history.push(current);
         switch (action.kind()) {
            case NEXT -> {
               ++current;
               if (current == wizard.pages().length) {
                  f.dispose();
                  cf.complete(values);
                  return;
               } else if (current == wizard.pages().length - 1) {
                  next.setText("Finish");
               }
            }
            case GOTO -> {
               for (current = 0; current < wizard.pages().length; current++) {
                  if (wizard.pages()[current].name().equals(action.name())) {
                     break;
                  }
               }
               if (current == wizard.pages().length) {
                  throw new IllegalArgumentException("Cannot jump to non-existing page: " + action.name());
               }
            }
         }
         back.setEnabled(current > 0);
         populate();
      });
      close.addActionListener(e -> {
         f.dispose();
         cf.complete(null);
      });
      main = new JPanel();
      f.add(main, BorderLayout.CENTER);
      center(f);
      populate();
      f.setVisible(true);
      try {
         return Optional.ofNullable(cf.get());
      } catch (InterruptedException | ExecutionException e) {
         throw new RuntimeException(e);
      }
   }

   private void formToValues() {
      Page page = wizard.pages()[current];
      for (int i = 0; i < page.inputs().length; i++) {
         Object o = suppliers.get(i).get();
         String prefix = page.name() + '.' + page.inputs()[i].name();
         if (o instanceof Collection<?> collection) {
            int j = 0;
            for (Object item : collection) {
               if (item instanceof Map<?, ?> map) { // tabular data
                  for (Map.Entry<?, ?> entry : map.entrySet()) {
                     values.setValue(prefix + '.' + j + '.' + entry.getKey(), entry.getValue());
                  }
               } else { // list data
                  values.setValue(prefix + '.' + j, item);
               }
               j++;
            }
         } else {
            values.setValue(prefix, o);
         }
      }
   }

   private void populate() {
      main.removeAll();
      Page page = wizard.pages()[current];
      main.setLayout(new GridBagLayout());
      main.setBorder(
            BorderFactory.createTitledBorder(
                  BorderFactory.createEtchedBorder(),
                  page.title(),
                  TitledBorder.DEFAULT_JUSTIFICATION,
                  TitledBorder.DEFAULT_POSITION,
                  main.getFont().deriveFont(
                        Font.BOLD,
                        AffineTransform.getScaleInstance(2, 2)
                  ),
                  null
            )
      );
      int y = 0;
      if (page.text() != null) {
         GridBagConstraints gbc = new GridBagConstraints();
         gbc.gridwidth = 3;
         main.add(new JLabel(page.text()), gbc);
         y++;
      }
      suppliers.clear();
      for (int i = 0; i < page.inputs().length; i++, y++) {
         Input input = page.inputs()[i];
         if (input instanceof Text text) {
            addFieldLabel(main, y, text.text());
            JTextField field = new JTextField(text.initialValue());
            suppliers.add(field::getText);
            addField(main, y, field);
         } else if (input instanceof Secret secret) {
            addFieldLabel(main, y, secret.text());
            JPanel panel = new JPanel(new BorderLayout());
            JPasswordField field = new JPasswordField(secret.initialValue());
            JToggleButton toggle = new JToggleButton("\uD83D\uDC41");
            toggle.setFocusable(false);
            toggle.setPreferredSize(new Dimension(30, field.getPreferredSize().height));
            toggle.addActionListener(e -> {
               if (toggle.isSelected()) {
                  // Show password
                  field.setEchoChar((char) 0);
               } else {
                  // Hide password
                  field.setEchoChar('•');
               }
            });
            panel.add(field, BorderLayout.CENTER);
            panel.add(toggle, BorderLayout.EAST);
            suppliers.add(field::getPassword);
            addField(main, y, panel);
         } else if (input instanceof Number number) {
            addFieldLabel(main, y, number.text());
            SpinnerNumberModel model = new SpinnerNumberModel(number.defaultValue(), 0, Integer.MAX_VALUE, 1);
            suppliers.add(model::getValue);
            addField(main, y, new JSpinner(model));
         } else if (input instanceof YesNo yesNo) {
            addFieldLabel(main, y, yesNo.text());
            JCheckBox field = new JCheckBox(yesNo.text());
            suppliers.add(field::isSelected);
            addField(main, y, field);
         } else if (input instanceof Choice choice) {
            addFieldLabel(main, y, choice.text());
            String[] items = Arrays.stream(choice.items()).map(Choice.Item::text).toArray(String[]::new);
            JComboBox<String> field = new JComboBox<>(items);
            suppliers.add(() -> choice.items()[field.getSelectedIndex()].name());
            addField(main, y, field);
         } else if (input instanceof Checkbox checkbox) {
            addFieldLabel(main, y, checkbox.text());
            JPanel checkboxes = new JPanel();
            checkboxes.setLayout(new GridLayout(checkbox.items().length, 1));
            for (Checkbox.Item item : checkbox.items()) {
               JCheckBox field = new JCheckBox(item.text());
               field.setSelected(item.selected());
               checkboxes.add(field);
            }
            suppliers.add(() -> {
               StringBuilder sb = new StringBuilder();
               for (int j = 0; j < checkboxes.getComponentCount(); j++) {
                  JCheckBox field = (JCheckBox) checkboxes.getComponent(j);
                  if (field.isSelected()) {
                     if (!sb.isEmpty()) {
                        sb.append(',');
                     }
                     sb.append(checkbox.items()[j].name());
                  }
               }
               return sb.toString();
            });
            addField(main, y, checkboxes);
         } else if (input instanceof Table table) {
            addFieldLabel(main, y, table.text());
            JPanel panel = new JPanel(new BorderLayout());
            String[] columns = Stream.of(table.cells()).map(Input::name).toArray(String[]::new);
            DefaultTableModel model = new DefaultTableModel(columns, 0);
            JTable t = new JTable(model);
            t.setPreferredScrollableViewportSize(new Dimension(400, 300));
            panel.add(new JScrollPane(t), BorderLayout.CENTER);
            JButton addButton = new JButton("Add");
            JButton removeButton = new JButton("Remove");
            // Add button functionality
            addButton.addActionListener(e -> {
               Object[] row = Stream.of(table.cells()).map(Input::value).toArray(Object[]::new);
               model.addRow(row);
            });
            removeButton.addActionListener(e -> {
               int selectedRow = t.getSelectedRow();
               if (selectedRow != -1) {
                  model.removeRow(selectedRow);
               } else {
                  JOptionPane.showMessageDialog(main, "Please select a row to remove", "No Row Selected", JOptionPane.WARNING_MESSAGE);
               }
            });
            JPanel buttons = new JPanel();
            buttons.add(addButton);
            buttons.add(removeButton);
            panel.add(buttons, BorderLayout.SOUTH);
            addField(main, y, panel);
            suppliers.add(() -> {
               List<Map<String, Object>> values = new ArrayList<>(model.getRowCount());
               for (int row = 0; row < model.getRowCount(); row++) {
                  Map<String, Object> map = new HashMap<>(model.getColumnCount());
                  for (int col = 0; col < model.getColumnCount(); col++) {
                     map.put(columns[col], model.getValueAt(row, col));
                  }
                  values.add(map);
               }
               return values;
            });
         } else if (input instanceof File file) {
            addFieldLabel(main, y, file.text());
            JPanel p = new JPanel(new BorderLayout());
            JTextField text = new JTextField();
            p.add(text, BorderLayout.CENTER);
            JButton browse = new JButton("Browse");
            p.add(browse, BorderLayout.EAST);
            addField(main, y, p);
            JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
            browse.addActionListener(e -> {
               switch (chooser.showOpenDialog(main)) {
                  case JFileChooser.APPROVE_OPTION -> {
                     Path path = chooser.getSelectedFile().toPath();
                     text.setText(path.toString());
                  }
                  case JFileChooser.CANCEL_OPTION -> {
                  }
               }
            });
            suppliers.add(text::getText);
         } else if (input instanceof Directory directory) {
            addFieldLabel(main, y, directory.text());
            JPanel p = new JPanel(new BorderLayout());
            JTextField text = new JTextField();
            p.add(text, BorderLayout.CENTER);
            JButton browse = new JButton("Browse");
            p.add(browse, BorderLayout.EAST);
            addField(main, y, p);
            text.setText(System.getProperty("user.dir"));
            JFileChooser chooser = new JFileChooser(System.getProperty("user.dir"));
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            browse.addActionListener(e -> {
               switch (chooser.showOpenDialog(main)) {
                  case JFileChooser.APPROVE_OPTION -> {
                     Path path = chooser.getSelectedFile().toPath();
                     text.setText(path.toString());
                  }
                  case JFileChooser.CANCEL_OPTION -> {
                  }
               }
            });
            suppliers.add(text::getText);
         }
      }
      final JPanel hspacer = new JPanel();
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.gridx = 1;
      gbc.gridy = 0;
      gbc.weighty = 0.1;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      main.add(hspacer, gbc);
      final JPanel vspacer = new JPanel();
      gbc = new GridBagConstraints();
      gbc.gridx = 0;
      gbc.gridy = y;
      gbc.weighty = 1.0;
      gbc.fill = GridBagConstraints.VERTICAL;
      main.add(vspacer, gbc);
      main.validate();
   }

   private void addField(JComponent component, int row, JComponent field) {
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.gridx = 2;
      gbc.gridy = row;
      gbc.weightx = 3.0;
      gbc.anchor = GridBagConstraints.WEST;
      gbc.fill = GridBagConstraints.HORIZONTAL;
      component.add(field, gbc);
   }

   private void addFieldLabel(JComponent component, int row, String text) {
      GridBagConstraints gbc = new GridBagConstraints();
      gbc.gridx = 0;
      gbc.gridy = row;
      gbc.weightx = 1.0;
      gbc.anchor = GridBagConstraints.EAST;
      component.add(new JLabel(text), gbc);
   }

   public static void center(Window frame) {
      Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
      int x = (int) ((dimension.getWidth() - frame.getWidth()) / 2);
      int y = (int) ((dimension.getHeight() - frame.getHeight()) / 2);
      frame.setLocation(x, y);
   }
}
