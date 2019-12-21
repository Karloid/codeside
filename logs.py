#!/usr/bin/env python3

import click
import subprocess
import json
import os
import time
import logging
import traceback
import multiprocessing
import glob

log = logging.getLogger()


@click.group()
def main():
	pass


def analyze(base_path):
	cwd = os.getcwd()

	full_path = cwd + "/" + base_path + ""
	os.chdir(full_path)

	while True:
		process(full_path)
		time.sleep(10)


def process(full_path):
	eliminations_a = 0
	eliminations_b = 0
	overscores_a = 0
	overscores_b = 0
	draws = 0

	idx = 0
	games = 0
	# for sw in ['_0', '_1']:
	for sw in ['_', '_swap_']:
		swap = idx == 1
		idx += 1

		# print("SW: " + sw + "\n\n")

		for file in glob.glob(f"{sw}result*.txt"):
			fp = full_path + file
			sz = int(os.path.getsize(fp))
			if sz == 0:
				# print("passing file " + fp)
				continue

			games += 1
			# print(fp)

			with open(fp, "r") as js:
				j = json.load(js)
				# print(j['results'])
				a = j['results'][0]
				b = j['results'][1]
				if swap:
					a, b = b, a

				if a > b:
					if a > 1000:
						eliminations_a += 1
					else:
						overscores_a += 1
				elif a == b:
					draws += 1
				else:
					if b > 1000:
						eliminations_b += 1
					else:
						overscores_b += 1

	total_a = eliminations_a + overscores_a
	total_b = eliminations_b + overscores_b
	print(total_a, " : ", total_b, "\t\t", eliminations_a, "/", overscores_a, " : ", eliminations_b, "/", overscores_b, "\tDraws: ", draws, "\t\tGames: ", games,
		  "\t", flush=True)


@main.command()
@click.option('--path', type=str, required=True)
def run(path):
	if not os.path.exists(path):
		print(dir, " not found.")
		return

	print("analyzing " + path)

	analyze(path)


if __name__ == "__main__":
	main()
