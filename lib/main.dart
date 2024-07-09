import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(MyApp());
}

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: Text('Audio Classifier'),
        ),
        body: AudioClassifierWidget(),
      ),
    );
  }
}

class AudioClassifierWidget extends StatefulWidget {
  const AudioClassifierWidget({super.key});
  @override
  State<AudioClassifierWidget> createState() => _AudioClassifierWidgetState();
}

class _AudioClassifierWidgetState extends State<AudioClassifierWidget> {
  static const platform = MethodChannel('com.example.audio_classifier');
  String _result = 'No results yet';
  String _error = '';

  int _numThreads = 2;
  int _numOfResults = 2;
  double _displayThreshold = 0.3;

  Future<void> startAudioClassification() async {
    try {
      await platform.invokeMethod('startAudioClassification');
    } on PlatformException catch (e) {
      setState(() {
        _error = "Failed to start audio classification: '${e.message}'.";
      });
    }
  }

  Future<void> stopAudioClassification() async {
    try {
      await platform.invokeMethod('stopAudioClassification');
    } on PlatformException catch (e) {
      setState(() {
        _error = "Failed to stop audio classification: '${e.message}'.";
      });
    }
  }

  Future<void> updateNumThreads() async {
    try {
      await platform.invokeMethod('updateNumThreads', {'numThreads': _numThreads});
    } on PlatformException catch (e) {
      setState(() {
        _error = "Failed to update number of threads: '${e.message}'.";
      });
    }
  }

  Future<void> updateNumOfResults() async {
    try {
      await platform.invokeMethod('updateNumOfResults', {'numOfResults': _numOfResults});
    } on PlatformException catch (e) {
      setState(() {
        _error = "Failed to update number of results: '${e.message}'.";
      });
    }
  }

  Future<void> updateDisplayThreshold() async {
    try {
      await platform.invokeMethod('updateDisplayThreshold', {'displayThreshold': _displayThreshold.toString()});
    } on PlatformException catch (e) {
      setState(() {
        _error = "Failed to update display threshold: '${e.message}'.";
      });
    }
  }

  @override
  void initState() {
    super.initState();
    platform.setMethodCallHandler((call) async {
      switch (call.method) {
        case 'onResult':
          setState(() {
            _result = call.arguments;
          });
          break;
        case 'onError':
          setState(() {
            _error = call.arguments;
          });
          break;
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        Text(_result),
        Text(_error),
        ElevatedButton(
          onPressed: startAudioClassification,
          child: Text('Start Classification'),
        ),
        ElevatedButton(
          onPressed: stopAudioClassification,
          child: Text('Stop Classification'),
        ),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text('Number of Threads: $_numThreads'),
            IconButton(
              icon: Icon(Icons.remove),
              onPressed: () {
                setState(() {
                  if (_numThreads > 1) _numThreads--;
                  updateNumThreads();
                });
              },
            ),
            IconButton(
              icon: Icon(Icons.add),
              onPressed: () {
                setState(() {
                  _numThreads++;
                  updateNumThreads();
                });
              },
            ),
          ],
        ),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text('Number of Results: $_numOfResults'),
            IconButton(
              icon: Icon(Icons.remove),
              onPressed: () {
                setState(() {
                  if (_numOfResults > 1) _numOfResults--;
                  updateNumOfResults();
                });
              },
            ),
            IconButton(
              icon: Icon(Icons.add),
              onPressed: () {
                setState(() {
                  _numOfResults++;
                  updateNumOfResults();
                });
              },
            ),
          ],
        ),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Text('Display Threshold: ${_displayThreshold.toStringAsFixed(1)}'),
            IconButton(
              icon: Icon(Icons.remove),
              onPressed: () {
                setState(() {
                  if (_displayThreshold > 0.1) _displayThreshold -= 0.1;
                  updateDisplayThreshold();
                });
              },
            ),
            IconButton(
              icon: Icon(Icons.add),
              onPressed: () {
                setState(() {
                  if (_displayThreshold < 1.0) _displayThreshold += 0.1;
                  updateDisplayThreshold();
                });
              },
            ),
          ],
        ),
      ],
    );
  }
}
