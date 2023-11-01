import 'dart:math';


import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:mobile_scanner_example/scanner_error_widget.dart';

class TestScanner extends StatefulWidget {
  const TestScanner({super.key, required this.returnImage, required this.scanWindow});
  final bool scanWindow;
  final bool returnImage;

  @override
  TestScannerState createState() =>
      TestScannerState();
}

class TestScannerState
    extends State<TestScanner> {
  late MobileScannerController controller = MobileScannerController(returnImage: widget.returnImage, formats: [BarcodeFormat.all], cameraResolution: const Size(640,480));
  Barcode? barcode;
  BarcodeCapture? capture;
  int index = 0;
  int detect = 0;

  Future<void> onDetect(BarcodeCapture barcode) async {
    HapticFeedback.heavyImpact();
    index++;
    detect = barcode.barcodes.length;
    capture = barcode;
    setState(() => this.barcode = barcode.barcodes.first);
  }

  MobileScannerArguments? arguments;

  @override
  Widget build(BuildContext context) {
    final size = MediaQuery.of(context).size;
    final scanWindow = Rect.fromCenter(
      center: size.center(Offset.zero),
      width: size.width * 0.8,
      height: size.width * 0.8,
    );

    return Scaffold(
      backgroundColor: Colors.black,
      body: Builder(
        builder: (context) {
          return Stack(
            fit: StackFit.expand,
            children: [
              MobileScanner(
                fit: BoxFit.cover,
                scanWindow: widget.scanWindow ? scanWindow : null,
                controller: controller,
                onScannerStarted: (arguments) {
                  setState(() {
                    this.arguments = arguments;
                  });
                },
                errorBuilder: (context, error, child) {
                  return ScannerErrorWidget(error: error);
                },
                onDetect: onDetect,
              ),
              if(widget.scanWindow) Container(
                decoration: ShapeDecoration(
                  shape: EzScannerOverlayShape(
                    borderRadius: 10,
                    borderColor:  Colors.amber,
                    borderLength: 40,
                    borderWidth: 10,
                    cutOutSize: size.width * 0.8,
                    overlayColor: Colors.black54,
                  ),
                ),
              ),
              Align(
                alignment: Alignment.bottomCenter,
                child: Container(
                  alignment: Alignment.bottomCenter,
                  height: 100,
                  color: Colors.black.withOpacity(0.4),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                    children: [
                      if(widget.returnImage) const Center(
                        child: FittedBox(
                          child:     Icon(
                            Icons.image,
                            color: Colors.white,
                          ),
                        ),
                      ),
                      if(widget.scanWindow) const Center(
                        child: FittedBox(
                          child:     Icon(
                            Icons.window,
                            color: Colors.white,
                          ),
                        ),
                      ),
                      Center(
                        child: FittedBox(
                          child: Text(
                            '$index',
                            overflow: TextOverflow.fade,
                            style: Theme.of(context)
                                .textTheme
                                .headlineMedium!
                                .copyWith(color: Colors.white),
                          ),
                        ),
                      ),
                      // Center(
                      //   child: FittedBox(
                      //     child: Text(
                      //       '$detect',
                      //       overflow: TextOverflow.fade,
                      //       style: Theme.of(context)
                      //           .textTheme
                      //           .headlineMedium!
                      //           .copyWith(color: Colors.white),
                      //     ),
                      //   ),
                      // ),
                      Center(
                        child: FittedBox(
                          child:     IconButton(
                            onPressed: () => controller.switchCamera(),
                            icon: const Icon(
                              Icons.cameraswitch_rounded,
                              color: Colors.white,
                            ),
                          ),
                        ),
                      ),
                      Center(
                        child: FittedBox(
                          child:  ValueListenableBuilder<TorchState>(
                            valueListenable: controller.torchState,
                            builder: (context, value, child) {
                              final Color iconColor;

                              switch (value) {
                                case TorchState.off:
                                  iconColor = Colors.white;
                                  break;
                                case TorchState.on:
                                  iconColor = Colors.yellow;
                                  break;
                              }

                              return IconButton(
                                onPressed: () => controller.toggleTorch(),
                                icon: Icon(
                                  Icons.flashlight_on,
                                  color: iconColor,
                                ),
                              );
                            },
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ],
          );
        },
      ),
    );
  }
}

class ScannerOverlay extends CustomPainter {
  ScannerOverlay(this.scanWindow);

  final Rect scanWindow;

  @override
  void paint(Canvas canvas, Size size) {
    final backgroundPath = Path()..addRect(Rect.largest);
    final cutoutPath = Path()..addRect(scanWindow);

    final backgroundPaint = Paint()
      ..color = Colors.black.withOpacity(0.5)
      ..style = PaintingStyle.fill
      ..blendMode = BlendMode.dstOut;

    final backgroundWithCutout = Path.combine(
      PathOperation.difference,
      backgroundPath,
      cutoutPath,
    );
    canvas.drawPath(backgroundWithCutout, backgroundPaint);
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) {
    return false;
  }
}

class EzScannerOverlayShape extends ShapeBorder {
  const EzScannerOverlayShape({
    this.borderColor = Colors.red,
    this.borderWidth = 4.0,
    this.overlayColor = const Color.fromRGBO(0, 0, 0, 80),
    this.borderRadius = 0,
    this.borderLength = 42,
    this.cutOutSize = 300,
    this.cutOutBottomOffset = 0,
  });

  /// Color of the border.
  final Color borderColor;

  /// Width of the border.
  final double borderWidth;

  /// Color of the overlay.
  final Color overlayColor;

  /// Radius of the border.
  final double borderRadius;

  /// Length of the border.
  final double borderLength;

  final double cutOutSize;

  /// Bottom offset of the cut out.
  final double cutOutBottomOffset;

  @override
  EdgeInsetsGeometry get dimensions => const EdgeInsets.all(10);

  @override
  Path getInnerPath(Rect rect, {TextDirection? textDirection}) {
    return Path()
      ..fillType = PathFillType.evenOdd
      ..addPath(getOuterPath(rect), Offset.zero);
  }

  @override
  Path getOuterPath(Rect rect, {TextDirection? textDirection}) {
    Path getLeftTopPath(Rect rect) {
      return Path()
        ..moveTo(rect.left, rect.bottom)
        ..lineTo(rect.left, rect.top)
        ..lineTo(rect.right, rect.top);
    }

    return getLeftTopPath(rect)
      ..lineTo(
        rect.right,
        rect.bottom,
      )
      ..lineTo(
        rect.left,
        rect.bottom,
      )
      ..lineTo(
        rect.left,
        rect.top,
      );
  }

  @override
  void paint(Canvas canvas, Rect rect, {TextDirection? textDirection}) {
    final width = rect.width;
    final borderWidthSize = width / 2;
    final height = rect.height;
    final borderOffset = borderWidth / 2;
    final bLength = borderLength > min(cutOutSize, cutOutSize) / 2 + borderWidth * 2 ? borderWidthSize / 2 : borderLength;
    final cutWidth = cutOutSize < width ? cutOutSize : width - borderOffset;
    final cutHeight = cutOutSize < height ? cutOutSize : height - borderOffset;

    final backgroundPaint = Paint()
      ..color = overlayColor
      ..style = PaintingStyle.fill;

    final borderPaint = Paint()
      ..color = borderColor
      ..style = PaintingStyle.stroke
      ..strokeWidth = borderWidth;

    final boxPaint = Paint()
      ..color = borderColor
      ..style = PaintingStyle.fill
      ..blendMode = BlendMode.dstOut;

    final cutOutRect = Rect.fromLTWH(
      rect.left + width / 2 - cutWidth / 2 + borderOffset,
      -cutOutBottomOffset + rect.top + height / 2 - cutHeight / 2 + borderOffset,
      cutWidth - borderOffset * 2,
      cutHeight - borderOffset * 2,
    );

    canvas
      ..saveLayer(
        rect,
        backgroundPaint,
      )
      ..drawRect(
        rect,
        backgroundPaint,
      )

    /// Draw top right corner
      ..drawRRect(
        RRect.fromLTRBAndCorners(
          cutOutRect.right - bLength,
          cutOutRect.top,
          cutOutRect.right,
          cutOutRect.top + bLength,
          topRight: Radius.circular(borderRadius),
        ),
        borderPaint,
      )

    /// Draw top left corner
      ..drawRRect(
        RRect.fromLTRBAndCorners(
          cutOutRect.left,
          cutOutRect.top,
          cutOutRect.left + bLength,
          cutOutRect.top + bLength,
          topLeft: Radius.circular(borderRadius),
        ),
        borderPaint,
      )

    /// Draw bottom right corner
      ..drawRRect(
        RRect.fromLTRBAndCorners(
          cutOutRect.right - bLength,
          cutOutRect.bottom - bLength,
          cutOutRect.right,
          cutOutRect.bottom,
          bottomRight: Radius.circular(borderRadius),
        ),
        borderPaint,
      )

    /// Draw bottom left corner
      ..drawRRect(
        RRect.fromLTRBAndCorners(
          cutOutRect.left,
          cutOutRect.bottom - bLength,
          cutOutRect.left + bLength,
          cutOutRect.bottom,
          bottomLeft: Radius.circular(borderRadius),
        ),
        borderPaint,
      )
      ..drawRRect(
        RRect.fromRectAndRadius(
          cutOutRect,
          Radius.circular(borderRadius),
        ),
        boxPaint,
      )
      ..restore();
  }

  @override
  ShapeBorder scale(double t) {
    return EzScannerOverlayShape(
      borderColor: borderColor,
      borderWidth: borderWidth,
      overlayColor: overlayColor,
    );
  }
}

class BorderPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    const width = 4.0;
    const radius = 20.0;
    const tRadius = 3 * radius;
    final rect = Rect.fromLTWH(
      width,
      width,
      size.width - 2 * width,
      size.height - 2 * width,
    );
    final rrect = RRect.fromRectAndRadius(rect, const Radius.circular(radius));
    const clippingRect0 = Rect.fromLTWH(
      0,
      0,
      tRadius,
      tRadius,
    );
    final clippingRect1 = Rect.fromLTWH(
      size.width - tRadius,
      0,
      tRadius,
      tRadius,
    );
    final clippingRect2 = Rect.fromLTWH(
      0,
      size.height - tRadius,
      tRadius,
      tRadius,
    );
    final clippingRect3 = Rect.fromLTWH(
      size.width - tRadius,
      size.height - tRadius,
      tRadius,
      tRadius,
    );

    final path = Path()
      ..addRect(clippingRect0)
      ..addRect(clippingRect1)
      ..addRect(clippingRect2)
      ..addRect(clippingRect3);

    canvas.clipPath(path);
    canvas.drawRRect(
      rrect,
      Paint()
        ..color = Colors.white
        ..style = PaintingStyle.stroke
        ..strokeWidth = width,
    );
  }

  @override
  bool shouldRepaint(CustomPainter oldDelegate) {
    return false;
  }
}
