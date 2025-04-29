<link rel="stylesheet" type="text/css" href="_styles/styles.css">

# Printing Settings

Intercross supports printing cross labels directly to Zebra mobile printers, allowing you to immediately label your crosses in the field.

<figure class="image">
    <img class="screenshot" src="_static/images/settings/printing_settings.png" width="350px">
    <figcaption class="screenshot-caption"><i>Printing settings screen</i></figcaption>
</figure>

## <img class="icon" src="_static/icons/printer.png"> Zebra Print Connect

Intercross integrates with the Zebra Print Connect app for printer communication. This allows printing to any supported Zebra printer over Bluetooth.

### Setting Up Zebra Print Connect

1. Install the [Zebra Print Connect app](https://play.google.com/store/apps/details?id=com.zebra.printconnect) from Google Play
2. Pair your Zebra printer with your device via Bluetooth
3. Open the Zebra Print Connect app and configure your printer
4. In Intercross, navigate to Settings > Printing
5. Toggle "Use Zebra Print Connect" to On

## <img class="icon" src="_static/icons/import.png"> ZPL Import

Zebra printers use ZPL (Zebra Programming Language) templates to format labels. Intercross allows you to import custom ZPL templates.

### Default Label Format

The default label includes:
- Cross ID (with barcode)
- Female parent ID
- Male parent ID
- Date
- Creator

## <img class="icon" src="_static/icons/import.png"> ZPL Import

<figure class="image">
    <img class="screenshot" src="_static/images/settings/printing_import_zpl.png" width="350px">
    <figcaption class="screenshot-caption"><i>ZPL import details</i></figcaption>
</figure>

It's possible to customize the label format by importing a custom zpl template.

1. Create a ZPL template using Zebra designer software or a text editor
2. Place the template file in the `/Intercross/templates/` directory
3. Navigate to Settings > Printing
4. Tap "Import ZPL Template"
5. Select your template file

### ZPL Template Variables

Your custom templates can use these variables:
- `{crossID}` - The cross identifier
- `{femaleID}` - Female parent ID
- `{maleID}` - Male parent ID
- `{date}` - Date of the cross
- `{person}` - Name of the person who made the cross
- `{experiment}` - Experiment name

## Printing a Label

With printing configured, you can print labels:
1. From the Cross Details screen by tapping the print icon
2. After creating a new cross by tapping the print button
3. In batch from the Events list by selecting multiple crosses

## Troubleshooting

If you encounter printing problems:
- Ensure your printer is charged and turned on
- Verify Bluetooth is enabled on your device
- Check that your printer is paired with your device
- Confirm Zebra Print Connect is properly configured
- Try restarting both your printer and device