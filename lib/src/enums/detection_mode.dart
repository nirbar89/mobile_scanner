/// The detection mode of the scanner.
enum DetectionMode {
  /// The scanner will act as a camera for personal use-cases
  noDetections(0),

  /// The barcode scanner will scan barcodes
  barcodes(1);

  const DetectionMode(this.rawValue);

  factory DetectionMode.fromRawValue(int value) {
    switch (value) {
      case 0:
        return DetectionMode.noDetections;
      case 1:
        return DetectionMode.barcodes;
      default:
        throw ArgumentError.value(value, 'value', 'Invalid raw value.');
    }
  }

  /// The raw value for the detection mode.
  final int rawValue;
}
