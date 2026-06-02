<?php

require_once 'fpdf/fpdf.php';
require_once 'fpdi/autoload.php';

// initiate FPDI
$pdf = new \setasign\Fpdi\Fpdi();
// add a page
$pdf->AddPage();
// set the source file
$pdf->setSourceFile(file: 'egs.pdf');
// import page 1
$tplIdx = $pdf->importPage(pageNumber: 1);
// use the imported page and place it at position 10,10 with a width of 100 mm
$pdf->useTemplate(tpl: $tplIdx, x: 10, y: 10, width: 100);

// now write some text above the imported page
$pdf->SetFont(family: 'Helvetica');
$pdf->SetTextColor(r: 255, g: 0, b: 0);
$pdf->SetXY(x: 20, y: 10);
$pdf->Write(h: 0, txt: 'Infomail Daniel Widmer');

//$pdf->Output(dest: 'I', name: 'egs_out.pdf');
$pdf->Output();
