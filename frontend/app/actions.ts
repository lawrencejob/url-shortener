'use server'

import { z } from 'zod'

const API_BASE_URL = process.env.API_BASE_URL ?? 'http://localhost:8080'

const schema = z.object({
    fullUrl: z
        .url()
        .trim()
        .refine((val) => /^https?:\/\//.test(val), {
            message: "URL must start with http:// or https://",
        }),
    alias: z
        .string()
        .trim()
        .nullish()
        .optional()
        .refine(
            (val) => !val || /^[a-zA-Z0-9]{4,32}$/.test(val),
            "Alias must be 4–32 characters of a-z, A-Z or 0-9"
        ),
})

type ShortenActionResult =
    | {
        status: null,
        fullUrl: string
        alias?: string
        errors: any
    }
    | {
        status: "invalid"
        fullUrl: string
        alias?: string
        errors: any
    }
    | {
        status: "created"
        shortUrl: string
    }
    | {
        status: "error"
        message: string
    }

export async function shortenAction(previousState: any, formData: FormData): Promise<ShortenActionResult> {
    console.log("Action called with:", previousState, formData)

    const fields = {
        fullUrl: formData.get('fullUrl')?.toString() || '',
        alias: formData.get('alias')?.toString() || '',
    }

    const validatedFields = schema.safeParse(fields)

    // Return early if the form data is invalid
    if (!validatedFields.success) {
        return {
            status: "invalid",
            ...fields,
            errors: z.treeifyError(validatedFields.error),
        }
    }

    const result = await fetch(`${API_BASE_URL}/shorten`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            fullUrl: validatedFields.data.fullUrl,
            customAlias: validatedFields.data.alias,
        }),
    })

    if (result.ok && result.status === 201) {
        const { shortUrl } = await result.json()

        return {
            status: "created",
            shortUrl
        }
    }

    try {
        const errorData = await result.json()
        return {
            status: "error",
            message: errorData?.message || 'An unknown error occurred',
        }
    } catch (e) {
        return {
            status: "error",
            message: 'An unknown error occurred',
        }
    }


}
