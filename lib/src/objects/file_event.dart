import 'package:mobile_scanner/src/enums/file_type.dart';

class FileEvent {
  final FileType fileType;
  final String? path;
  final int? rotationDegrees;
  FileEvent({required this.fileType,
    required this.path,
    this.rotationDegrees,
  });
}
